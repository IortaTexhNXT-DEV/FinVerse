package com.iortatechnxt.brokerverse.brokerclaims.insurer.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.BrokerClaimRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerClaim;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerClaimRepository;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerReserveChange;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerReserveChangeRepository;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The insurers of a claim incident (BRCLM.018/023/024/043; FR-CM-021/031/032): one line per insurer
 * and insurer claim number under one claim reference, the insurer's share, the number and date
 * reported to the insurer (no duplicate for the same insurer and claim, AC6; a warning and the
 * alert {@code BCL_INSURER_CLAIM_NO_REUSED} when another claim carries it), the insurer reserve
 * with its amendment history (information only, no journal) and the adjuster per line.
 */
@Service
@Transactional
public class InsurerClaimService {

  /** Exception code of an insurer claim number found on another claim. */
  public static final String REUSED = "BCL_INSURER_CLAIM_NO_REUSED";

  private static final String LINE = "InsurerClaim";

  private static final String INSURER = "Insurer ";

  private final InsurerClaimRepository lines;
  private final InsurerReserveChangeRepository reserves;
  private final BrokerClaimRepository claims;
  private final InsurerService insurers;
  private final LovService lovs;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param lines insurer lines
   * @param reserves reserve history
   * @param claims claims (numbers of other claims)
   * @param insurers insurer master
   * @param lovs adjusters
   * @param alerts reused number alert
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public InsurerClaimService(
      InsurerClaimRepository lines,
      InsurerReserveChangeRepository reserves,
      BrokerClaimRepository claims,
      InsurerService insurers,
      LovService lovs,
      AlertService alerts,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.lines = lines;
    this.reserves = reserves;
    this.claims = claims;
    this.insurers = insurers;
    this.lovs = lovs;
    this.alerts = alerts;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Adds an insurer line to a claim, with its number when known (FR-CM-021).
   *
   * @param claim open claim
   * @param line insurer, share, number, date reported and initial reserve
   * @param confirmReuse the user confirmed a number already on another claim
   * @return the line
   */
  public InsurerClaim add(Claim claim, NewLine line, boolean confirmReuse) {
    if (line.insurerCode() == null || line.insurerCode().isBlank()) {
      throw new BusinessRuleException("BCL_INSURER_REQUIRED", "Select the insurer");
    }
    String insurer =
        insurers.requireInsurer(claim.getCompanyId(), line.insurerCode().strip()).getPartyCode();
    String number = blankToNull(line.insurerClaimNo());
    checkNumber(claim, insurer, number, null, confirmReuse);
    InsurerClaim created =
        new InsurerClaim(
            claim.getCompanyId(), claim.getId(), insurer, line.sharePct(), line.reserve());
    created.number(number, line.reportedOn(), LocalDate.now(clock));
    InsurerClaim saved = lines.save(created);
    audit.record(
        ClaimCodes.ENTITY_TYPE,
        claim.getClaimNo(),
        AuditAction.UPDATE,
        INSURER + insurer + " added" + (number == null ? "" : " with claim number " + number));
    return saved;
  }

  /**
   * Records an insurer claim number on a line (FR-CM-021). A line that already has a number keeps
   * it: the new number becomes a further line of the same insurer.
   *
   * @param claim open claim
   * @param lineId line
   * @param number insurer claim number
   * @param reportedOn date reported to the insurer
   * @param confirmReuse the user confirmed a number already on another claim
   * @return the line carrying the number
   */
  public InsurerClaim number(
      Claim claim, Long lineId, String number, LocalDate reportedOn, boolean confirmReuse) {
    InsurerClaim line = require(claim, lineId);
    String value = blankToNull(number);
    if (value == null) {
      throw new BusinessRuleException(
          "BCL_INSURER_CLAIM_NO_REQUIRED", "Enter the insurer claim number");
    }
    if (line.getInsurerClaimNo() != null) {
      return add(
          claim, new NewLine(line.getInsurerCode(), null, value, reportedOn, null), confirmReuse);
    }
    checkNumber(claim, line.getInsurerCode(), value, line.getId(), confirmReuse);
    line.number(value, reportedOn, LocalDate.now(clock));
    audit.record(
        ClaimCodes.ENTITY_TYPE,
        claim.getClaimNo(),
        AuditAction.UPDATE,
        INSURER + line.getInsurerCode() + " claim number " + value);
    return line;
  }

  /**
   * Changes the share of an insurer (043 R3).
   *
   * @param claim open claim
   * @param lineId line
   * @param share share in percent
   * @return the line
   */
  public InsurerClaim changeShare(Claim claim, Long lineId, BigDecimal share) {
    InsurerClaim line = require(claim, lineId);
    BigDecimal previous = line.getSharePct();
    line.changeShare(share);
    audit.record(
        ClaimCodes.ENTITY_TYPE,
        claim.getClaimNo(),
        AuditAction.UPDATE,
        INSURER + line.getInsurerCode() + " share " + previous + " -> " + share);
    return line;
  }

  /**
   * Amends the insurer reserve of a line (BRCLM.023/024, BCL_RESERVE_AMEND); no journal (R2).
   *
   * @param claim open claim
   * @param lineId line
   * @param amount new reserve (0 allowed)
   * @param reason reason
   * @return the line
   */
  public InsurerClaim amendReserve(Claim claim, Long lineId, BigDecimal amount, String reason) {
    InsurerClaim line = require(claim, lineId);
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("BCL_REASON_REQUIRED", "Enter the reason for the change");
    }
    BigDecimal previous = line.amendReserve(amount);
    reserves.save(
        new InsurerReserveChange(
            line.getId(),
            previous,
            amount,
            reason.strip(),
            currentUser.username(),
            clock.instant()));
    audit.record(
        ClaimCodes.ENTITY_TYPE,
        claim.getClaimNo(),
        AuditAction.UPDATE,
        "Reserve of "
            + line.getInsurerCode()
            + " "
            + previous
            + " -> "
            + amount
            + ": "
            + reason.strip());
    return line;
  }

  /**
   * Sets the adjuster an insurer appointed on its line (BRCLM.018, BCL_ADJUSTER_ASSIGN).
   *
   * @param claim open claim
   * @param lineId line
   * @param adjusterCode adjuster ({@code BCL_ADJUSTER}, effective value)
   * @return the line
   */
  public InsurerClaim assignAdjuster(Claim claim, Long lineId, String adjusterCode) {
    InsurerClaim line = require(claim, lineId);
    if (adjusterCode == null || adjusterCode.isBlank()) {
      throw new BusinessRuleException("BCL_ADJUSTER_REQUIRED", "Select the adjuster");
    }
    lovs.requireValid(ClaimCodes.LOV_ADJUSTER, adjusterCode, LocalDate.now(clock));
    String previous = line.assignAdjuster(adjusterCode);
    audit.record(
        ClaimCodes.ENTITY_TYPE,
        claim.getClaimNo(),
        AuditAction.UPDATE,
        "Adjuster of " + line.getInsurerCode() + " " + previous + " -> " + adjusterCode);
    return line;
  }

  /**
   * The insurer lines of a claim.
   *
   * @param claimId claim
   * @return lines in the order added
   */
  @Transactional(readOnly = true)
  public List<InsurerClaim> ofClaim(Long claimId) {
    return lines.findByClaimIdOrderByIdAsc(claimId);
  }

  /**
   * The reserve history of insurer lines.
   *
   * @param lineIds lines
   * @return amendments, newest first
   */
  @Transactional(readOnly = true)
  public List<InsurerReserveChange> reserveHistory(Collection<Long> lineIds) {
    return lineIds.isEmpty()
        ? List.of()
        : reserves.findByInsurerClaimIdInOrderByChangedAtDescIdDesc(lineIds);
  }

  /**
   * One line of a claim.
   *
   * @param claim claim
   * @param lineId line
   * @return the line
   */
  @Transactional(readOnly = true)
  public InsurerClaim require(Claim claim, Long lineId) {
    return lines
        .findByIdAndClaimId(lineId, claim.getId())
        .orElseThrow(() -> new ResourceNotFoundException(LINE, lineId));
  }

  private void checkNumber(
      Claim claim, String insurer, String number, Long lineId, boolean confirmReuse) {
    List<InsurerClaim> same =
        number == null
            ? lines.findByClaimIdOrderByIdAsc(claim.getId()).stream()
                .filter(l -> l.getInsurerCode().equals(insurer) && l.getInsurerClaimNo() == null)
                .toList()
            : lines.findNumbered(claim.getCompanyId(), insurer, number);
    if (same.stream()
        .anyMatch(
            l -> l.getClaimId().equals(claim.getId()) && !Objects.equals(l.getId(), lineId))) {
      throw new BusinessRuleException(
          "BCL_INSURER_CLAIM_NO_DUPLICATE",
          number == null
              ? INSURER + insurer + " is already on this claim"
              : "Insurer claim number "
                  + number
                  + " is already recorded for "
                  + insurer
                  + " on this claim");
    }
    List<String> others =
        same.stream()
            .filter(l -> !l.getClaimId().equals(claim.getId()))
            .map(l -> claims.findById(l.getClaimId()).map(Claim::getClaimNo).orElse("?"))
            .distinct()
            .toList();
    if (number == null || others.isEmpty()) {
      return;
    }
    if (!confirmReuse) {
      throw new BusinessRuleException(
          REUSED,
          "Insurer claim number "
              + number
              + " of "
              + insurer
              + " is already on claim "
              + String.join(", ", others)
              + ". Confirm to continue");
    }
    alerts.raise(
        REUSED,
        new AlertFacts(
            claim.getCompanyId(),
            claim.getBranchId(),
            ClaimCodes.ENTITY_TYPE,
            String.valueOf(claim.getId()),
            "Insurer claim number "
                + number
                + " of "
                + insurer
                + " on "
                + claim.getClaimNo()
                + " is also on "
                + String.join(", ", others),
            null,
            REUSED + ":" + insurer + ":" + number));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * An insurer line to add.
   *
   * @param insurerCode insurer
   * @param sharePct share in percent, may be null
   * @param insurerClaimNo insurer claim number, may be null
   * @param reportedOn date reported to the insurer, may be null
   * @param reserve initial insurer reserve, may be null
   */
  public record NewLine(
      String insurerCode,
      BigDecimal sharePct,
      String insurerClaimNo,
      LocalDate reportedOn,
      BigDecimal reserve) {}
}
