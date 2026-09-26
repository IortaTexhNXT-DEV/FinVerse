package com.iortatechnxt.brokerverse.workflow.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Workflow stages. */
public interface WorkflowStageRepository extends JpaRepository<WorkflowStage, Long> {

  /**
   * Stages of a workflow in order.
   *
   * @param workflowCode workflow
   * @return stages
   */
  List<WorkflowStage> findByWorkflowCodeOrderBySortOrder(String workflowCode);

  /**
   * One stage.
   *
   * @param workflowCode workflow
   * @param stageCode stage
   * @return stage
   */
  Optional<WorkflowStage> findByWorkflowCodeAndStageCode(String workflowCode, String stageCode);

  /**
   * Every stage worked from a queue.
   *
   * @return stages with an owner permission
   */
  List<WorkflowStage>
      findByOwnerPermissionIsNotNullAndTerminalFalseOrderByWorkflowCodeAscSortOrderAsc();
}
