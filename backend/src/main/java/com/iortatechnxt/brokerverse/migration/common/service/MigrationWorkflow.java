package com.iortatechnxt.brokerverse.migration.common.service;

import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseRepository;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import org.springframework.stereotype.Component;

/**
 * Keeps the work case of a migration approval in step with the migration record (workflows {@code
 * MIG_OBJECT_DECISION}, {@code MIG_MAP_VERSION}, {@code MIG_BATCH_ROLLBACK}, {@code
 * MIG_OPENING_TRUEUP} and {@code MIG_RESUBMISSION}, V1080). The migration services check the
 * permissions and the segregation of duties themselves and then move the case as a system step, so
 * the case gives the approver's work queue and the history table of the record.
 */
@Component
public class MigrationWorkflow {

  private final WorkflowService workflow;
  private final WorkCaseRepository cases;

  /**
   * Creates the helper.
   *
   * @param workflow workflow engine
   * @param cases work cases
   */
  public MigrationWorkflow(WorkflowService workflow, WorkCaseRepository cases) {
    this.workflow = workflow;
    this.cases = cases;
  }

  /**
   * Opens the case of a record unless it exists.
   *
   * @param companyId company
   * @param workflowCode workflow
   * @param record record facts
   */
  public void open(Long companyId, String workflowCode, CaseRecord record) {
    if (cases.findByEntityTypeAndEntityId(record.entityType(), record.entityId()).isEmpty()) {
      workflow.start(new StartCase(companyId, workflowCode, record, null));
    }
  }

  /**
   * Moves the case of a record, when it has one.
   *
   * @param entityType entity type
   * @param entityId entity id
   * @param action workflow action
   * @param comment comment for the history
   */
  public void move(String entityType, String entityId, String action, String comment) {
    if (cases.findByEntityTypeAndEntityId(entityType, entityId).isPresent()) {
      workflow.systemTransition(entityType, entityId, action, TransitionNote.comment(comment));
    }
  }
}
