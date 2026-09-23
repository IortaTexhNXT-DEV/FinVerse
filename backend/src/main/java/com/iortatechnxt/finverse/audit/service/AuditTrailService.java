package com.iortatechnxt.finverse.audit.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.domain.AuditLog;
import com.iortatechnxt.finverse.audit.domain.AuditLogRepository;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the audit trail.
 *
 * <p>{@link #record} joins the caller's transaction so the audit row commits or rolls back with the
 * business change. {@link #recordIndependently} commits on its own and is used for events that must
 * survive a failed request (e.g. failed logins).
 */
@Service
public class AuditTrailService {

  private static final int MAX_SUMMARY = 500;

  private final AuditLogRepository repository;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param repository audit repository
   * @param currentUser current user resolver
   * @param clock system clock
   */
  public AuditTrailService(AuditLogRepository repository, CurrentUser currentUser, Clock clock) {
    this.repository = repository;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records an action performed by the current user within the current transaction.
   *
   * @param entityType entity type, e.g. "JournalBatch"
   * @param entityId entity id or business key
   * @param action action
   * @param summary description
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public void record(String entityType, Object entityId, AuditAction action, String summary) {
    save(currentUser.username(), entityType, entityId, action, summary);
  }

  /**
   * Records an action in a separate transaction on behalf of a named user.
   *
   * @param username acting user
   * @param entityType entity type
   * @param entityId entity id
   * @param action action
   * @param summary description
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordIndependently(
      String username, String entityType, Object entityId, AuditAction action, String summary) {
    save(username, entityType, entityId, action, summary);
  }

  private void save(
      String username, String entityType, Object entityId, AuditAction action, String summary) {
    String text = summary.length() > MAX_SUMMARY ? summary.substring(0, MAX_SUMMARY) : summary;
    String id = entityId == null ? null : String.valueOf(entityId);
    repository.save(new AuditLog(clock.instant(), username, entityType, id, action, text));
  }
}
