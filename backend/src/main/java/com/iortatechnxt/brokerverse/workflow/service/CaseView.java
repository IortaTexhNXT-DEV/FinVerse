package com.iortatechnxt.brokerverse.workflow.service;

import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseHistory;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowStage;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowTransition;
import java.util.List;
import java.util.Map;

/**
 * A case with its stage, the actions the current user may take and its history.
 *
 * @param workCase case
 * @param stage current stage
 * @param actions actions available to the current user
 * @param history status history, oldest first
 * @param stageNames names of all stages of the workflow by code (history display)
 */
public record CaseView(
    WorkCase workCase,
    WorkflowStage stage,
    List<WorkflowTransition> actions,
    List<WorkCaseHistory> history,
    Map<String, String> stageNames) {}
