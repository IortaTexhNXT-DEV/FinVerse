package com.iortatechnxt.brokerverse.screening.cases.api.dto;

import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * A case with its facts, SLA state and the actions the user may take (FR-SS-041 case page).
 *
 * @param row the list facts
 * @param clientType INDIVIDUAL or CORPORATE
 * @param triggerCode what opened the case
 * @param triggerReference the trigger's reference
 * @param templateType review template type
 * @param accountOfficer the client's account officer
 * @param investigator the investigator who submitted
 * @param returnedFrom the stage that returned the case
 * @param returnedBy who returned it
 * @param roundNo round
 * @param recommendation recommendation
 * @param strRequired STR flag
 * @param committeeDecision final committee decision
 * @param committeeDecidedAt when
 * @param committeeRound committee round
 * @param stageEnteredAt stage entry
 * @param reminderLeadHours SLA reminder lead
 * @param breached SLA breached
 * @param escalatedTo who was notified of the breach
 * @param versions configuration versions (match, risk, approval, assignment, SLA, validation)
 * @param actions the actions of the current user
 */
public record CaseDetail(
    CaseRow row,
    String clientType,
    String triggerCode,
    String triggerReference,
    String templateType,
    String accountOfficer,
    String investigator,
    String returnedFrom,
    String returnedBy,
    int roundNo,
    String recommendation,
    boolean strRequired,
    String committeeDecision,
    Instant committeeDecidedAt,
    int committeeRound,
    Instant stageEnteredAt,
    Integer reminderLeadHours,
    boolean breached,
    String escalatedTo,
    List<Long> versions,
    List<String> actions) {

  /**
   * Maps a case.
   *
   * @param c the case
   * @param now the time of the SLA state
   * @param actions the actions of the current user
   * @return the detail
   */
  public static CaseDetail from(ScreeningCase c, Instant now, List<String> actions) {
    return new CaseDetail(
        CaseRow.from(c, now),
        c.getClientType(),
        c.getTriggerCode(),
        c.getTriggerReference(),
        c.getTemplateType(),
        c.getAccountOfficer(),
        c.getInvestigator(),
        c.getReturnedFrom() == null ? null : c.getReturnedFrom().name(),
        c.getReturnedBy(),
        c.getRoundNo(),
        c.getRecommendation(),
        c.isStrRequired(),
        c.getCommitteeDecision(),
        c.getCommitteeDecidedAt(),
        c.getCommitteeRound(),
        c.getStageEnteredAt(),
        c.getReminderLeadHours(),
        c.isBreached(),
        c.getEscalatedTo(),
        Arrays.asList(
            c.getMatchVersionId(),
            c.getRiskVersionId(),
            c.getApprovalVersionId(),
            c.getAssignmentVersionId(),
            c.getSlaVersionId(),
            c.getValidationVersionId()),
        List.copyOf(actions));
  }
}
