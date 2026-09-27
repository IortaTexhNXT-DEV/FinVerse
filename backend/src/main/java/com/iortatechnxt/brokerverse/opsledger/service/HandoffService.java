package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoffRepository;
import java.time.Clock;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hand-offs of the default port adapters: work a port's owning module would do (issue an OR, set up
 * an unapplied item) is recorded for the Operations team to do by hand while that module is not
 * active, then closed with a note. Idempotent on (port, source module, source reference).
 */
@Service
@Transactional
public class HandoffService {

  private static final String ENTITY = "OpsHandoff";

  private final OpsHandoffRepository handoffs;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param handoffs hand-offs
   * @param notifications notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public HandoffService(
      OpsHandoffRepository handoffs,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.handoffs = handoffs;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records a hand-off (or returns the existing one).
   *
   * @param companyId company
   * @param port port name
   * @param team permission of the users who do the work (notified)
   * @param spec what has to be done
   * @return the hand-off
   */
  public OpsHandoff record(Long companyId, String port, String team, OpsHandoff.Spec spec) {
    return handoffs
        .findByPortAndSourceModuleAndSourceRef(port, spec.sourceModule(), spec.sourceRef())
        .orElseGet(() -> create(companyId, port, team, spec));
  }

  /**
   * The hand-off of a source transaction, open or closed.
   *
   * @param port port name
   * @param sourceModule module that asked
   * @param sourceRef its reference
   * @return hand-off, empty when none was recorded
   */
  @Transactional(readOnly = true)
  public Optional<OpsHandoff> find(String port, String sourceModule, String sourceRef) {
    return handoffs.findByPortAndSourceModuleAndSourceRef(port, sourceModule, sourceRef);
  }

  private OpsHandoff create(Long companyId, String port, String team, OpsHandoff.Spec spec) {
    OpsHandoff saved = handoffs.save(new OpsHandoff(companyId, port, spec));
    audit.record(ENTITY, saved.getId(), AuditAction.CREATE, port + ": " + spec.summary());
    notifications.notifyPermission(
        team,
        new Notice(
            "Work handed over: " + port,
            spec.summary(),
            "/operations/handoffs",
            ENTITY,
            String.valueOf(saved.getId())));
    return saved;
  }

  /**
   * Closes a hand-off once done by hand.
   *
   * @param id hand-off
   * @param note what was done
   * @return the hand-off
   */
  public OpsHandoff close(Long id, String note) {
    OpsHandoff handoff =
        handoffs.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    handoff.close(note.strip(), currentUser.username(), clock.instant());
    audit.record(ENTITY, id, AuditAction.CLOSE, handoff.getClosingNote());
    return handoff;
  }

  /**
   * Hand-offs of a company with a status, newest first.
   *
   * @param companyId company
   * @param status status
   * @param pageable page
   * @return hand-offs
   */
  @Transactional(readOnly = true)
  public Page<OpsHandoff> list(Long companyId, OpsHandoff.Status status, Pageable pageable) {
    return handoffs.findByCompanyIdAndStatusOrderByIdDesc(companyId, status, pageable);
  }
}
