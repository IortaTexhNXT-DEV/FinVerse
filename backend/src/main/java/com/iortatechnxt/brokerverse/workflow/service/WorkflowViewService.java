package com.iortatechnxt.brokerverse.workflow.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseHistoryRepository;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseRepository;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowTransition;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of the workflow: a record's case, its stage, the user's actions and history. */
@Service
@Transactional(readOnly = true)
public class WorkflowViewService {

  private final WorkCaseRepository cases;
  private final WorkCaseHistoryRepository history;
  private final WorkflowDefinitions definitions;
  private final CurrentUser currentUser;

  /**
   * Creates the service.
   *
   * @param cases cases
   * @param history status history
   * @param definitions stage and transition definitions
   * @param currentUser current user
   */
  public WorkflowViewService(
      WorkCaseRepository cases,
      WorkCaseHistoryRepository history,
      WorkflowDefinitions definitions,
      CurrentUser currentUser) {
    this.cases = cases;
    this.history = history;
    this.definitions = definitions;
    this.currentUser = currentUser;
  }

  /**
   * The case of a record, with actions for the current user and history.
   *
   * @param entityType record type
   * @param entityId record id
   * @return view, empty when the record has no case
   */
  public Optional<CaseView> view(String entityType, String entityId) {
    return cases.findByEntityTypeAndEntityId(entityType, entityId).map(this::view);
  }

  /**
   * The view of a case.
   *
   * @param workCase case
   * @return view
   */
  public CaseView view(WorkCase workCase) {
    List<WorkflowTransition> actions =
        workCase.isClosed()
            ? List.of()
            : definitions.transitionsFrom(workCase).stream()
                .filter(t -> t.allowedFor(currentUser::hasAuthority))
                .toList();
    return new CaseView(
        workCase,
        definitions.stageOf(workCase),
        actions,
        history.findByCaseIdOrderByIdAsc(workCase.getId()),
        definitions.stageNames(workCase.getWorkflowCode()));
  }

  /**
   * One case.
   *
   * @param caseId id
   * @return case
   */
  public WorkCase get(Long caseId) {
    return cases
        .findById(caseId)
        .orElseThrow(() -> new ResourceNotFoundException(WorkflowService.ENTITY, caseId));
  }

  /**
   * The current stage code of a record (guard for business actions).
   *
   * @param entityType record type
   * @param entityId record id
   * @return stage code
   */
  public String stageOf(String entityType, String entityId) {
    return cases
        .findByEntityTypeAndEntityId(entityType, entityId)
        .map(WorkCase::getStageCode)
        .orElseThrow(() -> new ResourceNotFoundException(entityType + " work item", entityId));
  }
}
