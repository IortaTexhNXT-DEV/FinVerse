package com.iortatechnxt.brokerverse.workflow.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowStage;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowStageRepository;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowTransition;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowTransitionRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Read access to the stage and transition definitions (seeded by migrations). */
@Component
@Transactional(readOnly = true)
public class WorkflowDefinitions {

  private final WorkflowStageRepository stages;
  private final WorkflowTransitionRepository transitions;

  /**
   * Creates the component.
   *
   * @param stages stages
   * @param transitions transitions
   */
  public WorkflowDefinitions(
      WorkflowStageRepository stages, WorkflowTransitionRepository transitions) {
    this.stages = stages;
    this.transitions = transitions;
  }

  /**
   * One stage.
   *
   * @param workflowCode workflow
   * @param stageCode stage
   * @return stage
   */
  public WorkflowStage stage(String workflowCode, String stageCode) {
    return stages
        .findByWorkflowCodeAndStageCode(workflowCode, stageCode)
        .orElseThrow(
            () -> new ResourceNotFoundException("Workflow stage", workflowCode + ":" + stageCode));
  }

  /**
   * The stage a case is in.
   *
   * @param workCase case
   * @return stage
   */
  public WorkflowStage stageOf(WorkCase workCase) {
    return stage(workCase.getWorkflowCode(), workCase.getStageCode());
  }

  /**
   * The initial stage of a workflow.
   *
   * @param workflowCode workflow
   * @return stage
   */
  public WorkflowStage initialStage(String workflowCode) {
    return stages.findByWorkflowCodeOrderBySortOrder(workflowCode).stream()
        .filter(WorkflowStage::isInitial)
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Workflow", workflowCode));
  }

  /**
   * Stage names of a workflow by code.
   *
   * @param workflowCode workflow
   * @return names in stage order
   */
  public Map<String, String> stageNames(String workflowCode) {
    Map<String, String> names = new LinkedHashMap<>();
    stages
        .findByWorkflowCodeOrderBySortOrder(workflowCode)
        .forEach(s -> names.put(s.getStageCode(), s.getName()));
    return names;
  }

  /**
   * Transitions out of a case's current stage.
   *
   * @param workCase case
   * @return transitions in display order
   */
  public List<WorkflowTransition> transitionsFrom(WorkCase workCase) {
    return transitions.findByWorkflowCodeAndFromStageOrderBySortOrder(
        workCase.getWorkflowCode(), workCase.getStageCode());
  }

  /**
   * The transition of an action from the case's current stage.
   *
   * @param workCase case
   * @param action action code
   * @return transition
   */
  public WorkflowTransition requireTransition(WorkCase workCase, String action) {
    return transitions
        .findByWorkflowCodeAndFromStageAndAction(
            workCase.getWorkflowCode(), workCase.getStageCode(), action)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "WORKFLOW_TRANSITION_NOT_ALLOWED",
                    "'"
                        + action
                        + "' is not allowed while "
                        + workCase.getReference()
                        + " is in stage "
                        + stageOf(workCase).getName()));
  }
}
