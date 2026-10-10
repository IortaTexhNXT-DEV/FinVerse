package com.iortatechnxt.brokerverse.renewal.allocation.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerAllocation;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerAllocationRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Multiple insurer allocation of a renewal account (FRRN.014.03): the insurers of the renewal by
 * percentage (totalling 100%) or by coverage amount (totalling the total sum insured), with the
 * premium of each insurer computed from its share; without an allocation the renewal has its one
 * insurer at 100%.
 */
@Service
@Transactional
public class InsurerAllocationService {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int SCALE = 4;

  private final RenewalRecords records;
  private final InsurerAllocationRepository allocations;
  private final NamedParameterJdbcTemplate jdbc;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param allocations shares
   * @param jdbc insurers of the company
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public InsurerAllocationService(
      RenewalRecords records,
      InsurerAllocationRepository allocations,
      NamedParameterJdbcTemplate jdbc,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.records = records;
    this.allocations = allocations;
    this.jdbc = jdbc;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The insurers of a renewal account with their share and premium.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @return shares
   */
  @Transactional(readOnly = true)
  public List<Share> of(Long companyId, String renewalRef) {
    return of(records.get(companyId, renewalRef));
  }

  /**
   * The insurers of a renewal with their share and premium.
   *
   * @param c renewal
   * @return shares, the one insurer at 100% without an allocation
   */
  @Transactional(readOnly = true)
  public List<Share> of(RenewalCandidate c) {
    BigDecimal premium = premium(c);
    List<InsurerAllocation> saved = allocations.findByCandidateIdOrderByIdAsc(c.getId());
    if (saved.isEmpty()) {
      return List.of(new Share(c.getSnapshot().insurerCode(), HUNDRED, tsi(c), premium));
    }
    return saved.stream()
        .map(
            a ->
                new Share(
                    a.getInsurerCode(),
                    a.getSharePercent(),
                    a.getCoverageAmount(),
                    portion(premium, a.getSharePercent())))
        .toList();
  }

  /**
   * Allocates a renewal account to insurers, by percentage or by coverage amount.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @param shares insurers with a percentage or an amount each
   * @return the allocation
   */
  public List<Share> allocate(Long companyId, String renewalRef, List<Input> shares) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    if (shares == null || shares.isEmpty()) {
      throw new BusinessRuleException("RNW_ALLOCATION_EMPTY", "Add the insurers of the renewal");
    }
    requireInsurers(companyId, shares);
    boolean byAmount = shares.stream().allMatch(s -> s.amount() != null);
    BigDecimal tsi = tsi(c);
    requireTotal(shares, byAmount, tsi);
    allocations.deleteByCandidateId(c.getId());
    allocations.flush();
    for (Input s : shares) {
      BigDecimal percent =
          byAmount
              ? s.amount().multiply(HUNDRED).divide(tsi, SCALE, RoundingMode.HALF_UP)
              : s.percent();
      allocations.save(
          new InsurerAllocation(
              c.getId(),
              s.insurerCode().strip(),
              percent,
              byAmount ? s.amount() : null,
              currentUser.username(),
              clock.instant()));
    }
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.UPDATE,
        "Allocated to " + shares.size() + " insurers " + (byAmount ? "by amount" : "by share"));
    return of(c);
  }

  private static void requireTotal(List<Input> shares, boolean byAmount, BigDecimal tsi) {
    if (byAmount) {
      BigDecimal total =
          shares.stream().map(Input::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
      if (tsi == null || total.compareTo(tsi) != 0) {
        throw new BusinessRuleException(
            "RNW_ALLOCATION_TOTAL",
            "The coverage amounts must total the total sum insured of " + tsi);
      }
      return;
    }
    BigDecimal total =
        shares.stream()
            .map(s -> s.percent() == null ? BigDecimal.ZERO : s.percent())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.compareTo(HUNDRED) != 0) {
      throw new BusinessRuleException("RNW_ALLOCATION_TOTAL", "The shares must total 100%");
    }
  }

  private void requireInsurers(Long companyId, List<Input> shares) {
    Set<String> seen = new HashSet<>();
    shares.forEach(s -> requireInsurer(companyId, s, seen));
  }

  private void requireInsurer(Long companyId, Input s, Set<String> seen) {
    String code = s.insurerCode() == null ? "" : s.insurerCode().strip();
    if (code.isEmpty() || !seen.add(code)) {
      throw new BusinessRuleException("RNW_ALLOCATION_INSURER", "Select each insurer once");
    }
    if (!known(companyId, code)) {
      throw new BusinessRuleException("RNW_ALLOCATION_INSURER", code + " is not an insurer");
    }
    requirePositive(code, s);
  }

  private static void requirePositive(String code, Input s) {
    if (negative(s.percent()) || negative(s.amount())) {
      throw new BusinessRuleException(
          "RNW_ALLOCATION_VALUE", "The share of " + code + " must be positive");
    }
  }

  private boolean known(Long companyId, String code) {
    Integer count =
        jdbc.queryForObject(
            "select count(*) from cat_insurer where company_id = :companyId and party_code = :code",
            Map.of("companyId", companyId, "code", code),
            Integer.class);
    return count != null && count > 0;
  }

  private static boolean negative(BigDecimal v) {
    return v != null && v.signum() <= 0;
  }

  private static BigDecimal tsi(RenewalCandidate c) {
    CandidateSnapshot.SnapshotPremium p = c.getSnapshot().premium();
    return p == null ? null : p.totalSumInsured();
  }

  private static BigDecimal premium(RenewalCandidate c) {
    CandidateSnapshot.SnapshotPremium p = c.getSnapshot().premium();
    return p == null ? null : p.grossPremium();
  }

  private static BigDecimal portion(BigDecimal premium, BigDecimal percent) {
    return premium == null
        ? null
        : premium.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
  }

  /**
   * An insurer's share as entered.
   *
   * @param insurerCode insurer
   * @param percent share in percent, or null
   * @param amount coverage amount, or null
   */
  public record Input(String insurerCode, BigDecimal percent, BigDecimal amount) {}

  /**
   * An insurer's share.
   *
   * @param insurerCode insurer
   * @param percent share in percent
   * @param amount coverage amount, may be null
   * @param premium premium of the insurer, may be null
   */
  public record Share(
      String insurerCode, BigDecimal percent, BigDecimal amount, BigDecimal premium) {}
}
