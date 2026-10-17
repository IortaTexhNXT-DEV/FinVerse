package com.iortatechnxt.brokerverse.submitted.proposal.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNominatedRate;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNominatedRateRepository;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The nominated package rates of Submitted Policies Setup (FR-SP-066): per segment, vehicle
 * classification and insurer, maker-checker. The rate of a record is the authorized rate in force
 * of its segment and insurer for its classification, else for every classification.
 */
@Service
@Transactional
public class NominatedRateService {

  /** Audit entity. */
  public static final String ENTITY = "SbmNominatedRate";

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final SbmNominatedRateRepository rates;
  private final InsurerService insurers;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param rates nominated rates
   * @param insurers insurer master
   * @param lovs lists of values (segments)
   * @param audit audit trail
   * @param currentUser signed-in user
   * @param clock clock
   */
  public NominatedRateService(
      SbmNominatedRateRepository rates,
      InsurerService insurers,
      LovService lovs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.rates = rates;
    this.insurers = insurers;
    this.lovs = lovs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The rates of a company.
   *
   * @param companyId company
   * @return rates
   */
  @Transactional(readOnly = true)
  public List<SbmNominatedRate> list(Long companyId) {
    return rates.findByCompanyIdOrderBySegmentAscVehicleTypeAscInsurerCodeAscIdAsc(companyId);
  }

  /**
   * Adds a rate, pending authorization.
   *
   * @param companyId company
   * @param row values
   * @return rate
   */
  public SbmNominatedRate create(Long companyId, SbmNominatedRate.Row row) {
    SbmNominatedRate.Row checked = check(row);
    lovs.requireValid(SubmittedCodes.LOV_SEGMENT, checked.segment(), BusinessClock.today(clock));
    insurers.requireInsurer(companyId, checked.insurerCode());
    SbmNominatedRate saved = rates.save(new SbmNominatedRate(companyId, checked));
    audit.record(ENTITY, saved.getId(), AuditAction.CREATE, describe(saved));
    return saved;
  }

  /**
   * Changes the rate or dates; it must be authorized again.
   *
   * @param companyId company
   * @param id rate
   * @param row new values
   * @return rate
   */
  public SbmNominatedRate update(Long companyId, Long id, SbmNominatedRate.Row row) {
    SbmNominatedRate rate = row(companyId, id);
    rate.change(check(row));
    audit.record(ENTITY, id, AuditAction.UPDATE, describe(rate));
    return rate;
  }

  /**
   * Authorizes a rate (a checker other than its maker).
   *
   * @param companyId company
   * @param id rate
   * @return rate
   */
  public SbmNominatedRate authorize(Long companyId, Long id) {
    SbmNominatedRate rate = row(companyId, id);
    rate.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, id, AuditAction.AUTHORIZE, describe(rate));
    return rate;
  }

  /**
   * Deactivates a rate.
   *
   * @param companyId company
   * @param id rate
   * @return rate
   */
  public SbmNominatedRate deactivate(Long companyId, Long id) {
    SbmNominatedRate rate = row(companyId, id);
    rate.deactivate();
    audit.record(ENTITY, id, AuditAction.DEACTIVATE, describe(rate));
    return rate;
  }

  /**
   * The nominated rate of a segment, classification and insurer on a date.
   *
   * @param companyId company
   * @param segment segment of the record
   * @param vehicleType vehicle classification of the record, may be null
   * @param insurerCode insurer
   * @param date date
   * @return rate in percent, empty when none is nominated
   */
  @Transactional(readOnly = true)
  public Optional<BigDecimal> rateOf(
      Long companyId, String segment, String vehicleType, String insurerCode, LocalDate date) {
    if (segment == null || insurerCode == null) {
      return Optional.empty();
    }
    return rates.findByCompanyIdAndSegmentAndInsurerCode(companyId, segment, insurerCode).stream()
        .filter(r -> r.inForce(date))
        .filter(r -> r.getVehicleType() == null || same(r.getVehicleType(), vehicleType))
        .min(Comparator.comparing(r -> r.getVehicleType() == null ? 1 : 0))
        .map(SbmNominatedRate::getRate);
  }

  private static boolean same(String a, String b) {
    return b != null
        && a.strip().toLowerCase(Locale.ROOT).equals(b.strip().toLowerCase(Locale.ROOT));
  }

  private SbmNominatedRate row(Long companyId, Long id) {
    return rates
        .findById(id)
        .filter(r -> r.getCompanyId().equals(companyId))
        .orElseThrow(() -> new ResourceNotFoundException("Nominated rate", id));
  }

  private static SbmNominatedRate.Row check(SbmNominatedRate.Row row) {
    if (blank(row.segment())
        || blank(row.insurerCode())
        || row.rate() == null
        || row.effectiveFrom() == null) {
      throw new BusinessRuleException(
          "SBM_NOMINATED_RATE_INCOMPLETE",
          "Enter the segment, the insurer, the rate and the start date");
    }
    requireRange(row.rate());
    return new SbmNominatedRate.Row(
        row.segment().strip(),
        blank(row.vehicleType()) ? null : row.vehicleType().strip(),
        row.insurerCode().strip(),
        row.rate(),
        row.effectiveFrom(),
        row.effectiveTo());
  }

  private static void requireRange(BigDecimal rate) {
    if (rate.signum() <= 0 || rate.compareTo(HUNDRED) > 0) {
      throw new BusinessRuleException(
          "SBM_NOMINATED_RATE_RANGE", "The rate must be between 0 and 100 percent");
    }
  }

  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }

  private static String describe(SbmNominatedRate r) {
    return "Nominated rate "
        + r.getRate().stripTrailingZeros().toPlainString()
        + "% for "
        + r.getSegment()
        + (r.getVehicleType() == null ? "" : " / " + r.getVehicleType())
        + " with "
        + r.getInsurerCode();
  }
}
