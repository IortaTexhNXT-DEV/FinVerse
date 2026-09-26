package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimClosureKind;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPhase;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimProgress;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimEvent;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimEventRepository;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimField;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.StatusHistory;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.StatusHistoryRepository;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side of the status engine for the claim record (FR-CL-003/042/053): where the claim stands
 * with its ages computed on read, and its History tab (status changes with the days spent in each
 * earlier status, and the field changes of the claim timeline).
 */
@Service
@Transactional(readOnly = true)
public class ClaimProgressQuery {

  private final ClaimLookup lookup;
  private final StatusRules rules;
  private final StatusHistoryRepository history;
  private final ClaimEventRepository events;
  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the query.
   *
   * @param lookup claims of the company
   * @param rules status attributes and labels
   * @param history status history
   * @param events claim timeline
   * @param lovs lists of values
   * @param clock clock
   */
  public ClaimProgressQuery(
      ClaimLookup lookup,
      StatusRules rules,
      StatusHistoryRepository history,
      ClaimEventRepository events,
      LovService lovs,
      Clock clock) {
    this.lookup = lookup;
    this.rules = rules;
    this.history = history;
    this.events = events;
    this.lovs = lovs;
    this.clock = clock;
  }

  /**
   * Where a claim stands.
   *
   * @param companyId company
   * @param claimId claim
   * @return progress with ages as of today
   */
  public Progress progress(Long companyId, Long claimId) {
    Claim claim = lookup.require(companyId, claimId);
    ClaimProgress p = claim.getProgress();
    LocalDate today = ClaimAgeing.today(clock);
    return new Progress(
        claim.getId(),
        claim.getClaimNo(),
        claim.getHandler(),
        new Status(
            p.getStatusCode(),
            rules.statusLabel(p.getStatusCode()),
            p.getPhase(),
            p.getStatusSince(),
            p.getClosureKind(),
            rules.awaitsPremiumRemittance(p.getStatusCode())),
        new Ages(
            ClaimAgeing.ageThisStage(p.getStatusSince(), today),
            ClaimAgeing.ageOverall(claim.getLoss().getReportedDate(), p.getClosedOn(), today)),
        new SettlementView(
            p.getSettlementTypeCode(),
            rules.settlementLabel(p.getSettlementTypeCode()),
            p.getSettlementAmount(),
            p.getDateSettled(),
            p.getClosedOn()),
        new FollowUp(
            p.getNextFollowUpDate(),
            p.isFollowUpOverridden(),
            p.getNextActionPlan(),
            p.getAdjusterCode(),
            p.getAdjusterCode() == null
                ? null
                : lovs.label(ClaimCodes.LOV_ADJUSTER, p.getAdjusterCode())));
  }

  /**
   * The History tab of a claim.
   *
   * @param companyId company
   * @param claimId claim
   * @return status changes and field changes, oldest first
   */
  public History history(Long companyId, Long claimId) {
    Claim claim = lookup.require(companyId, claimId);
    List<StatusChange> changes =
        history.findByClaimIdOrderByChangedAtAscIdAsc(claim.getId()).stream()
            .map(this::change)
            .toList();
    List<FieldChange> fields =
        events.findByClaimIdOrderByChangedAtAscIdAsc(claim.getId()).stream()
            .map(ClaimProgressQuery::field)
            .toList();
    return new History(changes, fields);
  }

  private StatusChange change(StatusHistory h) {
    return new StatusChange(
        h.getFromStatus(),
        h.getFromStatus() == null ? null : rules.statusLabel(h.getFromStatus()),
        h.getFromPhase(),
        h.getToStatus(),
        rules.statusLabel(h.getToStatus()),
        h.getToPhase(),
        new Stamp(h.getChangedBy(), h.getChangedAt(), h.getRemark()),
        h.getDaysInPrevious());
  }

  private static FieldChange field(ClaimEvent e) {
    return new FieldChange(
        e.getField(),
        e.getOldValue(),
        e.getNewValue(),
        new Stamp(e.getChangedBy(), e.getChangedAt(), e.getReason()));
  }

  /**
   * Where a claim stands.
   *
   * @param claimId claim
   * @param claimNo claim number
   * @param handler claims handler
   * @param status status, phase and closure
   * @param ages age this stage and overall
   * @param settlement settlement and closure date
   * @param followUp follow-up, action plan and adjuster
   */
  public record Progress(
      Long claimId,
      String claimNo,
      String handler,
      Status status,
      Ages ages,
      SettlementView settlement,
      FollowUp followUp) {}

  /**
   * Status of a claim.
   *
   * @param code status code
   * @param label status label
   * @param phase phase
   * @param since time the status was set
   * @param closureKind TEMPORARY or PERMANENT when closed
   * @param awaitingPremiumRemittance whether the status awaits the premium remittance
   */
  public record Status(
      String code,
      String label,
      ClaimPhase phase,
      Instant since,
      ClaimClosureKind closureKind,
      boolean awaitingPremiumRemittance) {}

  /**
   * Ages of a claim in calendar days.
   *
   * @param thisStage days in the current status
   * @param overall days since the reported date (to the closure date when closed)
   */
  public record Ages(int thisStage, int overall) {}

  /**
   * Settlement of a claim.
   *
   * @param typeCode requested type of settlement
   * @param typeLabel label
   * @param amount settlement amount
   * @param dateSettled date settled
   * @param closedOn closure date
   */
  public record SettlementView(
      String typeCode,
      String typeLabel,
      BigDecimal amount,
      LocalDate dateSettled,
      LocalDate closedOn) {}

  /**
   * Follow-up of a claim.
   *
   * @param nextFollowUpDate next follow-up date
   * @param overridden whether the date was overridden
   * @param nextActionPlan next action plan summary
   * @param adjusterCode adjuster
   * @param adjusterName adjuster label
   */
  public record FollowUp(
      LocalDate nextFollowUpDate,
      boolean overridden,
      String nextActionPlan,
      String adjusterCode,
      String adjusterName) {}

  /**
   * The History tab.
   *
   * @param statusChanges status changes
   * @param fieldChanges field changes
   */
  public record History(List<StatusChange> statusChanges, List<FieldChange> fieldChanges) {}

  /**
   * Who, when and why.
   *
   * @param by user
   * @param at time
   * @param remark remark or reason
   */
  public record Stamp(String by, Instant at, String remark) {}

  /**
   * A status change.
   *
   * @param fromStatus previous status
   * @param fromLabel previous status label
   * @param fromPhase previous phase
   * @param toStatus new status
   * @param toLabel new status label
   * @param toPhase new phase
   * @param stamp user, time and remark
   * @param daysInPrevious days spent in the previous status
   */
  public record StatusChange(
      String fromStatus,
      String fromLabel,
      ClaimPhase fromPhase,
      String toStatus,
      String toLabel,
      ClaimPhase toPhase,
      Stamp stamp,
      Integer daysInPrevious) {}

  /**
   * A field change.
   *
   * @param field field
   * @param oldValue previous value
   * @param newValue new value
   * @param stamp user, time and reason
   */
  public record FieldChange(ClaimField field, String oldValue, String newValue, Stamp stamp) {}
}
