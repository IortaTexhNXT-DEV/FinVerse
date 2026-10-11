package com.iortatechnxt.brokerverse.workflow.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Workflow transitions. */
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {

  /**
   * Transitions out of a stage.
   *
   * @param workflowCode workflow
   * @param fromStage stage
   * @return transitions in display order
   */
  List<WorkflowTransition> findByWorkflowCodeAndFromStageOrderBySortOrder(
      String workflowCode, String fromStage);

  /**
   * One transition.
   *
   * @param workflowCode workflow
   * @param fromStage stage
   * @param action action
   * @return transition
   */
  Optional<WorkflowTransition> findByWorkflowCodeAndFromStageAndAction(
      String workflowCode, String fromStage, String action);

  /**
   * Every transition of a workflow.
   *
   * @param workflowCode workflow
   * @return transitions
   */
  List<WorkflowTransition> findByWorkflowCodeOrderByFromStageAscSortOrderAsc(String workflowCode);
}
