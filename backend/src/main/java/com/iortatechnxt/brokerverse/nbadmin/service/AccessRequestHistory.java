package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestAction;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestEvent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestEventRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the history of an access request (BRD 1.008, 2.002; every remark of the BRD) and the
 * matching audit trail entry, in the transaction of the step.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class AccessRequestHistory {

  private final AccessRequestEventRepository events;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the component.
   *
   * @param events history
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public AccessRequestHistory(
      AccessRequestEventRepository events,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.events = events;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records a step: the history event (with remarks) and the audit trail entry.
   *
   * @param r request after the step
   * @param action what happened
   * @param from status before the step, null for a new request
   * @param remarks remarks, null for none
   */
  public void record(
      AccessRequest r, AccessRequestAction action, AccessRequestStatus from, String remarks) {
    events.save(
        new AccessRequestEvent(
            r.getId(),
            action,
            from,
            r.getStatus(),
            remarks,
            currentUser.username(),
            clock.instant()));
    audit.record(
        AccessRequestService.ENTITY,
        r.getRequestNo(),
        auditAction(action),
        action.label()
            + ": "
            + AccessRequestService.describe(r)
            + AccessRequestService.note(remarks));
  }

  /**
   * History of a request, oldest first.
   *
   * @param requestId request
   * @return events
   */
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<AccessRequestEvent> of(Long requestId) {
    return events.findByRequestIdOrderByIdAsc(requestId);
  }

  private static AuditAction auditAction(AccessRequestAction action) {
    return switch (action) {
      case SAVE -> AuditAction.CREATE;
      case EDIT, SCHEDULE, APPLY_FAILED, FOR_IMPLEMENTATION -> AuditAction.UPDATE;
      case SUBMIT, RESUBMIT -> AuditAction.SUBMIT;
      case RETURN, REJECT -> AuditAction.REJECT;
      case CANCEL -> AuditAction.DEACTIVATE;
      case APPROVE, SECOND_APPROVE, APPLY, IMPLEMENT -> AuditAction.AUTHORIZE;
    };
  }
}
