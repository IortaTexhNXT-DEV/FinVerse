package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimEvent;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimField;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.FollowUp;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.FollowUpRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records a follow-up on a claim (FRS BRD-07 FRCLM.009.04): the follow-up type and the action taken
 * today are required; a status other than the current one changes the status of the claim through
 * the status engine (access matrix, next follow-up date, history); the next action plan summary
 * becomes the plan of the claim. The follow-up count and the date of the last follow-up feed the
 * worklist (FRCLM.009.01) and the quick view (FRCLM.009.03).
 */
@Service
@Transactional
public class FollowUpRecorder {

  private static final int MAX_SUMMARY = 500;
  private static final int MAX_TEXT = 2000;

  private final ClaimLookup lookup;
  private final ClaimStatusService statuses;
  private final ClaimFollowUpService followUps;
  private final FollowUpRepository repository;
  private final ClaimEventRecorder recorder;
  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the recorder.
   *
   * @param lookup claims of the company
   * @param statuses status engine
   * @param followUps next action plan
   * @param repository follow-ups
   * @param recorder claim timeline
   * @param lovs follow-up types
   * @param clock clock
   */
  public FollowUpRecorder(
      ClaimLookup lookup,
      ClaimStatusService statuses,
      ClaimFollowUpService followUps,
      FollowUpRepository repository,
      ClaimEventRecorder recorder,
      LovService lovs,
      Clock clock) {
    this.lookup = lookup;
    this.statuses = statuses;
    this.followUps = followUps;
    this.repository = repository;
    this.recorder = recorder;
    this.lovs = lovs;
    this.clock = clock;
  }

  /**
   * Records a follow-up.
   *
   * @param companyId company
   * @param claimId claim
   * @param request entry, status and amounts
   * @return the follow-up
   */
  public FollowUp record(Long companyId, Long claimId, Request request) {
    Claim claim = lookup.require(companyId, claimId);
    ClaimLookup.requireOpen(claim, "it");
    FollowUp.Entry entry = request.entry();
    LocalDate today = ClaimAgeing.today(clock);
    validate(request, today);
    FollowUp.Amounts amounts = request.amounts();
    String status = request.statusCode();
    if (status != null
        && !status.isBlank()
        && !status.equals(claim.getProgress().getStatusCode())) {
      statuses.change(companyId, claimId, status, entry.actionTaken().strip());
    }
    if (entry.nextPlanSummary() != null && !entry.nextPlanSummary().isBlank()) {
      followUps.planNextAction(companyId, claimId, entry.nextPlanSummary());
    }
    FollowUp saved =
        repository.save(
            new FollowUp(
                claim.getId(),
                new FollowUp.Entry(
                    entry.type(),
                    entry.actionTaken().strip(),
                    strip(entry.nextPlanSummary()),
                    strip(entry.nextPlanDetail())),
                amounts,
                claim.getProgress().getStatusCode(),
                today));
    recorder.record(
        claim,
        ClaimField.FOLLOW_UP_RECORDED,
        new ClaimEvent.Values(null, lovs.label(ClaimCodes.LOV_FOLLOW_UP_TYPE, entry.type())),
        saved.getActionTaken());
    return saved;
  }

  /**
   * Follow-ups of a claim, newest first.
   *
   * @param companyId company
   * @param claimId claim
   * @return follow-ups
   */
  @Transactional(readOnly = true)
  public List<FollowUp> of(Long companyId, Long claimId) {
    lookup.require(companyId, claimId);
    return repository.findByClaimIdOrderByIdDesc(claimId);
  }

  private void validate(Request request, LocalDate today) {
    FollowUp.Entry entry = request.entry();
    if (entry.type() == null || entry.type().isBlank()) {
      throw invalid("BCL_FOLLOW_UP_TYPE_REQUIRED", "Select the follow-up type");
    }
    lovs.requireValid(ClaimCodes.LOV_FOLLOW_UP_TYPE, entry.type(), today);
    if (entry.actionTaken() == null || entry.actionTaken().isBlank()) {
      throw invalid("BCL_ACTION_TAKEN_REQUIRED", "Enter the action taken today");
    }
    requireLength(entry.actionTaken(), MAX_TEXT, "action taken");
    requireLength(entry.nextPlanSummary(), MAX_SUMMARY, "next action plan summary");
    requireLength(entry.nextPlanDetail(), MAX_TEXT, "next action plan detail");
    FollowUp.Amounts amounts = request.amounts();
    if (Stream.of(
            amounts.settlementAmount(), amounts.adjusterEvaluation(), amounts.insurerDeductible())
        .anyMatch(a -> a != null && a.signum() < 0)) {
      throw invalid("BCL_AMOUNT_NEGATIVE", "The amounts of a follow-up cannot be negative");
    }
  }

  private static void requireLength(String text, int max, String what) {
    if (text != null && text.strip().length() > max) {
      throw invalid(
          "BCL_FOLLOW_UP_TOO_LONG", "The " + what + " can have up to " + max + " characters");
    }
  }

  private static String strip(String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }

  private static BusinessRuleException invalid(String code, String message) {
    return new BusinessRuleException(code, message);
  }

  /**
   * A follow-up to record.
   *
   * @param entry type, action taken and next plan
   * @param statusCode status as of the date, the current one when blank
   * @param amounts settlement amount, adjuster's evaluation, insurer's deductible
   */
  public record Request(FollowUp.Entry entry, String statusCode, FollowUp.Amounts amounts) {

    /** Empty amounts when none are given. */
    public Request {
      amounts = amounts == null ? new FollowUp.Amounts(null, null, null) : amounts;
    }
  }
}
