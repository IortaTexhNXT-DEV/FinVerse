package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPhase;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimProgress;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimEvent;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimField;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.StatusHistory;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Settlement, permanent closure and reopen of a claim (BRCLM.005/014/015/029/035, FR-CL-044/045;
 * CLAIMS_BROKING_DESIGN 8.1). The requested type of settlement is set by a TL / TH; a type that
 * needs it requires the settlement amount and the date settled (not in the future); a type that
 * closes the claim also needs {@code BCL_CLOSE} and closes it permanently (phase CLOSED, closure
 * PERMANENT, closure date). A permanently closed claim is reopened with {@code BCL_REOPEN} and a
 * reason: it is in progress again and the settlement is cleared, kept in the claim history.
 */
@Service
@Transactional
public class ClaimClosureService {

  private static final String REOPEN_REASON_REQUIRED = "WORKFLOW_REASON_REQUIRED";

  private final ClaimLookup lookup;
  private final StatusRules rules;
  private final StatusTransitions transitions;
  private final ClaimEventRecorder recorder;
  private final LovService lovs;
  private final NotificationService notifications;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param lookup claims of the company
   * @param rules settlement attributes
   * @param transitions history, workflow, audit, notice and event of a phase change
   * @param recorder claim timeline
   * @param lovs lists of values
   * @param notifications in-app notifications
   * @param currentUser current user
   * @param clock clock
   */
  public ClaimClosureService(
      ClaimLookup lookup,
      StatusRules rules,
      StatusTransitions transitions,
      ClaimEventRecorder recorder,
      LovService lovs,
      NotificationService notifications,
      CurrentUser currentUser,
      Clock clock) {
    this.lookup = lookup;
    this.rules = rules;
    this.transitions = transitions;
    this.recorder = recorder;
    this.lovs = lovs;
    this.notifications = notifications;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Sets or changes the requested type of settlement (FR-CL-044); a closing type closes the claim.
   *
   * @param companyId company
   * @param claimId claim
   * @param settlement type, amount, date and remark
   * @return the claim
   */
  public Claim settle(Long companyId, Long claimId, Settlement settlement) {
    Claim claim = lookup.require(companyId, claimId);
    ClaimLookup.requireOpen(claim, "it");
    String type = settlement.typeCode();
    if (type == null || type.isBlank()) {
      throw new BusinessRuleException(
          "BCL_SETTLEMENT_TYPE_REQUIRED", "Select the type of settlement");
    }
    LocalDate today = ClaimAgeing.today(clock);
    lovs.requireValid(ClaimCodes.LOV_SETTLEMENT_TYPE, type, today);
    StatusRules.SettlementRule rule = rules.settlementRule(type);
    validate(settlement, rule, today);
    if (rule.closesClaim() && !currentUser.hasAuthority(Permission.BCL_CLOSE.name())) {
      throw new AccessDeniedException("You are not permitted to perform this action");
    }
    ClaimProgress progress = claim.getProgress();
    String before = describe(progress);
    BigDecimal amount =
        settlement.amount() == null ? null : settlement.amount().setScale(2, RoundingMode.HALF_UP);
    progress.settle(type, amount, settlement.dateSettled());
    recorder.record(
        claim,
        ClaimField.SETTLEMENT,
        new ClaimEvent.Values(before, describe(progress)),
        settlement.remark());
    if (rule.closesClaim()) {
      close(claim, "Closed with settlement type " + rules.settlementLabel(type), settlement);
    }
    return claim;
  }

  /**
   * Reopens a permanently closed claim (FR-CL-045).
   *
   * @param companyId company
   * @param claimId claim
   * @param reasonCode reason ({@code BCL_REOPEN_REASON})
   * @param remark remark, may be null
   * @return the claim
   */
  public Claim reopen(Long companyId, Long claimId, String reasonCode, String remark) {
    Claim claim = lookup.require(companyId, claimId);
    if (!claim.isClosed()) {
      throw new BusinessRuleException(
          "BCL_CLAIM_NOT_CLOSED", "Claim " + claim.getClaimNo() + " is not closed");
    }
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new BusinessRuleException(REOPEN_REASON_REQUIRED, "Select a reason for 'reopen'");
    }
    lovs.requireValid(ClaimCodes.LOV_REOPEN_REASON, reasonCode, ClaimAgeing.today(clock));
    ClaimProgress progress = claim.getProgress();
    StatusHistory.Step from = new StatusHistory.Step(progress.getStatusCode(), ClaimPhase.CLOSED);
    Instant since = progress.getStatusSince();
    String before = describe(progress);
    progress.reopen(clock.instant());
    LocalDate today = ClaimAgeing.today(clock);
    progress.scheduleFollowUp(today.plusDays(rules.followUpDays(progress.getStatusCode())), today);
    String reason = lovs.label(ClaimCodes.LOV_REOPEN_REASON, reasonCode);
    recorder.record(
        claim,
        ClaimField.SETTLEMENT,
        new ClaimEvent.Values(before, null),
        "Reopened: " + reason + (remark == null || remark.isBlank() ? "" : " - " + remark));
    transitions.record(claim, from, since, new TransitionNote(reasonCode, remark));
    notifyHandler(claim, reason);
    return claim;
  }

  private void close(Claim claim, String text, Settlement settlement) {
    ClaimProgress progress = claim.getProgress();
    if (progress.getStatusCode() == null) {
      throw new BusinessRuleException(
          "BCL_STATUS_REQUIRED", "Set the status of claim " + claim.getClaimNo() + " first");
    }
    StatusHistory.Step from = new StatusHistory.Step(progress.getStatusCode(), progress.getPhase());
    progress.close(ClaimAgeing.today(clock));
    String remark = settlement.remark();
    transitions.record(
        claim,
        from,
        progress.getStatusSince(),
        TransitionNote.comment(remark == null || remark.isBlank() ? text : text + " - " + remark));
  }

  private static void validate(Settlement s, StatusRules.SettlementRule rule, LocalDate today) {
    if (rule.requiresAmount() && (s.amount() == null || s.dateSettled() == null)) {
      throw new BusinessRuleException(
          "BCL_SETTLEMENT_AMOUNT_REQUIRED", "Enter the settlement amount and the date settled");
    }
    if (s.amount() != null && s.amount().signum() < 0) {
      throw new BusinessRuleException(
          "BCL_SETTLEMENT_AMOUNT_NEGATIVE", "The settlement amount cannot be negative");
    }
    if (s.dateSettled() != null && s.dateSettled().isAfter(today)) {
      throw new BusinessRuleException(
          "BCL_SETTLED_DATE_FUTURE", "The date settled cannot be in the future");
    }
  }

  private String describe(ClaimProgress progress) {
    if (progress.getSettlementTypeCode() == null) {
      return null;
    }
    StringBuilder text = new StringBuilder(rules.settlementLabel(progress.getSettlementTypeCode()));
    if (progress.getSettlementAmount() != null) {
      text.append("; amount ").append(progress.getSettlementAmount().toPlainString());
    }
    if (progress.getDateSettled() != null) {
      text.append("; settled ").append(progress.getDateSettled());
    }
    return text.toString();
  }

  private void notifyHandler(Claim claim, String reason) {
    if (CurrentUser.sameUser(claim.getHandler(), currentUser.username())) {
      return;
    }
    notifications.notifyUser(
        claim.getHandler(),
        new Notice(
            claim.getClaimNo() + " reopened",
            "Reason: " + reason,
            "/claims-handling/" + claim.getId(),
            ClaimCodes.ENTITY_TYPE,
            String.valueOf(claim.getId())));
  }

  /**
   * A requested type of settlement.
   *
   * @param typeCode settlement type ({@code BCL_SETTLEMENT_TYPE})
   * @param amount settlement amount (insurer's figure), may be null
   * @param dateSettled date settled, may be null
   * @param remark remark, may be null
   */
  public record Settlement(
      String typeCode, BigDecimal amount, LocalDate dateSettled, String remark) {}
}
