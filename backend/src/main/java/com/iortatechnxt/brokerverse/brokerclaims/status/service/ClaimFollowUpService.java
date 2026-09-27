package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimProgress;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimEvent;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimField;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Follow-up of an open claim (BRCLM.018-021, FR-CM-050/051): the override of the next follow-up
 * date with a reason (TL / TH), the next action plan summary (officers, TL, TH; up to 2,000
 * characters, every version kept) and the adjuster / appraiser of the claim (TL / TH). Each change
 * is kept in the claim timeline and the audit trail.
 */
@Service
@Transactional
public class ClaimFollowUpService {

  /** Longest next action plan summary (BRCLM.020). */
  public static final int MAX_ACTION_PLAN = 2000;

  private final ClaimLookup lookup;
  private final ClaimEventRecorder recorder;
  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param lookup claims of the company
   * @param recorder claim timeline
   * @param lovs lists of values
   * @param clock clock
   */
  public ClaimFollowUpService(
      ClaimLookup lookup, ClaimEventRecorder recorder, LovService lovs, Clock clock) {
    this.lookup = lookup;
    this.recorder = recorder;
    this.lovs = lovs;
    this.clock = clock;
  }

  /**
   * Overrides the next follow-up date (FR-CM-050); the override is kept across status changes until
   * its date passes.
   *
   * @param companyId company
   * @param claimId claim
   * @param date new date, today or later
   * @param reasonCode reason ({@code BCL_OVERRIDE_REASON})
   * @return the claim
   */
  public Claim overrideFollowUp(Long companyId, Long claimId, LocalDate date, String reasonCode) {
    Claim claim = lookup.require(companyId, claimId);
    ClaimLookup.requireOpen(claim, "it");
    LocalDate today = ClaimAgeing.today(clock);
    if (date == null) {
      throw new BusinessRuleException("BCL_FOLLOW_UP_REQUIRED", "Enter the next follow-up date");
    }
    if (date.isBefore(today)) {
      throw new BusinessRuleException(
          "BCL_FOLLOW_UP_PAST", "The follow-up date cannot be before today");
    }
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new BusinessRuleException("BCL_REASON_REQUIRED", "Enter the reason for the change");
    }
    lovs.requireValid(ClaimCodes.LOV_OVERRIDE_REASON, reasonCode, today);
    ClaimProgress progress = claim.getProgress();
    String before = text(progress.getNextFollowUpDate());
    progress.overrideFollowUp(date);
    recorder.record(
        claim,
        ClaimField.FOLLOW_UP,
        new ClaimEvent.Values(before, date.toString()),
        lovs.label(ClaimCodes.LOV_OVERRIDE_REASON, reasonCode));
    return claim;
  }

  /**
   * Encodes the next action plan summary (FR-CM-051).
   *
   * @param companyId company
   * @param claimId claim
   * @param text summary, blank clears it
   * @return the claim
   */
  public Claim planNextAction(Long companyId, Long claimId, String text) {
    Claim claim = lookup.require(companyId, claimId);
    ClaimLookup.requireOpen(claim, "it");
    String plan = text == null || text.isBlank() ? null : text.strip();
    if (plan != null && plan.length() > MAX_ACTION_PLAN) {
      throw new BusinessRuleException(
          "BCL_ACTION_PLAN_TOO_LONG", "The action plan can have up to 2000 characters");
    }
    ClaimProgress progress = claim.getProgress();
    String before = progress.getNextActionPlan();
    progress.planNextAction(plan);
    recorder.record(claim, ClaimField.ACTION_PLAN, new ClaimEvent.Values(before, plan), null);
    return claim;
  }

  /**
   * Sets or clears the adjuster / appraiser of the claim (BRCLM.018).
   *
   * @param companyId company
   * @param claimId claim
   * @param adjusterCode adjuster ({@code BCL_ADJUSTER}), blank clears it
   * @param remark remark, may be null
   * @return the claim
   */
  public Claim assignAdjuster(Long companyId, Long claimId, String adjusterCode, String remark) {
    Claim claim = lookup.require(companyId, claimId);
    ClaimLookup.requireOpen(claim, "it");
    String code = adjusterCode == null || adjusterCode.isBlank() ? null : adjusterCode;
    if (code != null) {
      lovs.requireValid(ClaimCodes.LOV_ADJUSTER, code, ClaimAgeing.today(clock));
    }
    ClaimProgress progress = claim.getProgress();
    String before = progress.getAdjusterCode();
    if (!Objects.equals(before, code)) {
      progress.assignAdjuster(code);
      recorder.record(
          claim,
          ClaimField.ADJUSTER,
          new ClaimEvent.Values(adjuster(before), adjuster(code)),
          remark);
    }
    return claim;
  }

  private String adjuster(String code) {
    return code == null ? null : lovs.label(ClaimCodes.LOV_ADJUSTER, code);
  }

  private static String text(LocalDate date) {
    return date == null ? null : date.toString();
  }
}
