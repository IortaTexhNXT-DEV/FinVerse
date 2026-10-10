package com.iortatechnxt.brokerverse.audit.service;

import com.iortatechnxt.brokerverse.audit.domain.ActorContext;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.domain.AuditLog;
import com.iortatechnxt.brokerverse.audit.domain.AuditLogRepository;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the audit trail.
 *
 * <p>{@link #record} joins the caller's transaction so the audit row commits or rolls back with the
 * business change. {@link #recordIndependently} commits on its own and is used for events that must
 * survive a failed request (e.g. failed logins).
 *
 * <p>Every entry carries the source address of the request and the roles of the actor at the time
 * of the action ({@link ActorContext}); the roles of a user acting outside a signed-in request (a
 * sign-in) are looked up through {@link ActorRoles}.
 */
@Service
public class AuditTrailService {

  private static final int MAX_SUMMARY = 500;

  private final AuditLogRepository repository;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final ObjectProvider<ActorRoles> roles;

  /**
   * Creates the service.
   *
   * @param repository audit repository
   * @param currentUser current user resolver
   * @param clock system clock
   * @param roles roles of a user, when the request does not know them
   */
  public AuditTrailService(
      AuditLogRepository repository,
      CurrentUser currentUser,
      Clock clock,
      ObjectProvider<ActorRoles> roles) {
    this.repository = repository;
    this.currentUser = currentUser;
    this.clock = clock;
    this.roles = roles;
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
   * Records a change of one value by the current user within the current transaction, with the
   * value before and after (shown as From and To on the Audit Trail).
   *
   * @param entityType entity type
   * @param entityId entity id or business key
   * @param action action
   * @param summary description
   * @param before value before the change, may be null
   * @param after value after the change, may be null
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public void recordChange(
      String entityType,
      Object entityId,
      AuditAction action,
      String summary,
      String before,
      String after) {
    repository.save(
        build(currentUser.username(), entityType, entityId, action, summary).values(before, after));
  }

  /**
   * Records an action by the current user with its remarks (the comment of an approval, a return or
   * a rejection), shown in the Remarks column of the audit logs (BDOI FRS FRPM.021).
   *
   * @param entityType entity type
   * @param entityId entity id or business key
   * @param action action
   * @param summary description
   * @param remarks remarks, may be null
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public void recordWithRemarks(
      String entityType, Object entityId, AuditAction action, String summary, String remarks) {
    repository.save(
        build(currentUser.username(), entityType, entityId, action, summary).remarks(remarks));
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

  /**
   * Audit history of one record known under several keys (e.g. a client's prospect and client
   * codes), newest first, at most 500 entries.
   *
   * @param entityType entity type
   * @param entityIds keys of the record
   * @return entries
   */
  @Transactional(readOnly = true)
  public List<AuditLog> history(String entityType, Collection<String> entityIds) {
    return repository.findTop500ByEntityTypeAndEntityIdInOrderByOccurredAtDesc(
        entityType, entityIds);
  }

  private void save(
      String username, String entityType, Object entityId, AuditAction action, String summary) {
    entry(username, entityType, entityId, action, summary);
  }

  private AuditLog entry(
      String username, String entityType, Object entityId, AuditAction action, String summary) {
    return repository.save(build(username, entityType, entityId, action, summary));
  }

  /** An entry not yet saved: the values and remarks are set before the insert-only save. */
  private AuditLog build(
      String username, String entityType, Object entityId, AuditAction action, String summary) {
    String text = summary.length() > MAX_SUMMARY ? summary.substring(0, MAX_SUMMARY) : summary;
    String id = entityId == null ? null : String.valueOf(entityId);
    return new AuditLog(clock.instant(), username, entityType, id, action, text)
        .from(ActorContext.address(), rolesOf(username));
  }

  private String rolesOf(String username) {
    String known = ActorContext.roles();
    if (known != null || CurrentUser.SYSTEM.equals(username)) {
      return known;
    }
    ActorRoles lookup = roles.getIfAvailable();
    return lookup == null ? null : lookup.rolesOf(username);
  }
}
