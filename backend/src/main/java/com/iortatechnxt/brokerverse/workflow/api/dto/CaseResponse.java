package com.iortatechnxt.brokerverse.workflow.api.dto;

import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseHistory;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowStage;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowTransition;
import com.iortatechnxt.brokerverse.workflow.service.CaseView;
import java.time.Instant;
import java.util.List;

/**
 * A record's workflow position: stage, SLA, assignee, available actions and status history.
 *
 * @param item the case
 * @param stageTerminal whether the stage is final
 * @param slaHours stage SLA
 * @param actions actions available to the current user
 * @param history status history, oldest first
 * @param stages every stage of the workflow in its defined order (the stepper of the record)
 */
public record CaseResponse(
    WorkItemResponse item,
    boolean stageTerminal,
    Integer slaHours,
    List<ActionInfo> actions,
    List<HistoryEntry> history,
    List<StageInfo> stages) {

  /**
   * Maps a case view.
   *
   * @param view view
   * @param now current time
   * @return response
   */
  public static CaseResponse from(CaseView view, Instant now) {
    var names = view.stageNames();
    return new CaseResponse(
        WorkItemResponse.from(view.workCase(), view.stage().getName(), now),
        view.stage().isTerminal(),
        view.stage().getSlaHours(),
        view.actions().stream().map(t -> ActionInfo.from(t, names.get(t.getToStage()))).toList(),
        view.history().stream()
            .map(h -> HistoryEntry.from(h, names.get(h.getFromStage()), names.get(h.getToStage())))
            .toList(),
        view.stages().stream().map(StageInfo::from).toList());
  }

  /**
   * A stage of the workflow.
   *
   * @param code stage code
   * @param name stage name
   * @param initial whether cases start in it
   * @param terminal whether it is final
   */
  public record StageInfo(String code, String name, boolean initial, boolean terminal) {

    static StageInfo from(WorkflowStage s) {
      return new StageInfo(s.getStageCode(), s.getName(), s.isInitial(), s.isTerminal());
    }
  }

  /**
   * An action.
   *
   * @param action action code
   * @param label button label
   * @param toStage target stage
   * @param toStageName target stage name
   * @param generic runnable from the generic workflow API
   * @param reasonLov list of values of the mandatory reason, null when none
   */
  public record ActionInfo(
      String action,
      String label,
      String toStage,
      String toStageName,
      boolean generic,
      String reasonLov) {

    static ActionInfo from(WorkflowTransition t, String toStageName) {
      return new ActionInfo(
          t.getAction(),
          t.getLabel(),
          t.getToStage(),
          toStageName,
          t.isGeneric(),
          t.getReasonLov());
    }
  }

  /**
   * A status change.
   *
   * @param fromStage previous stage
   * @param fromStageName previous stage name
   * @param toStage new stage
   * @param toStageName new stage name
   * @param action action
   * @param reasonCode reason
   * @param comment comment
   * @param actor user
   * @param automatic system action
   * @param occurredAt time
   */
  public record HistoryEntry(
      String fromStage,
      String fromStageName,
      String toStage,
      String toStageName,
      String action,
      String reasonCode,
      String comment,
      String actor,
      boolean automatic,
      Instant occurredAt) {

    static HistoryEntry from(WorkCaseHistory h, String fromName, String toName) {
      return new HistoryEntry(
          h.getFromStage(),
          fromName,
          h.getToStage(),
          toName,
          h.getAction(),
          h.getReasonCode(),
          h.getComment(),
          h.getActor(),
          h.isAutomatic(),
          h.getOccurredAt());
    }
  }
}
