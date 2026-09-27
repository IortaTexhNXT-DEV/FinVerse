package com.iortatechnxt.brokerverse.system.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.system.domain.SystemParameter;
import com.iortatechnxt.brokerverse.system.domain.SystemParameterRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Changes of the business parameters from the System Parameters screen. A parameter of the SECURITY
 * category (sign-in, password, lock-out, session and access settings, the emergency path
 * UAM_DIRECT_ROLE_EDIT among them) is not changed at once: the change waits until a holder of
 * SECURITY_PARAMETER_APPROVE other than the requester approves it (V1065). Other parameters change
 * at once, as before.
 */
@Service
@Transactional
public class SecurityParameterApprovals {

  /** Permission of the approvers. */
  public static final String APPROVE = "SECURITY_PARAMETER_APPROVE";

  private static final String ENTITY = "SystemParameter";

  private final SystemParameterRepository repository;
  private final SystemParameterService parameters;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param repository parameters
   * @param parameters applies approved values (audit and cache)
   * @param audit audit trail
   * @param currentUser current user
   * @param events event of a change to approve
   * @param clock clock
   */
  public SecurityParameterApprovals(
      SystemParameterRepository repository,
      SystemParameterService parameters,
      AuditTrailService audit,
      CurrentUser currentUser,
      ApplicationEventPublisher events,
      Clock clock) {
    this.repository = repository;
    this.parameters = parameters;
    this.audit = audit;
    this.currentUser = currentUser;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Changes a parameter from the screen: at once, or pending approval for a security parameter.
   *
   * @param key key
   * @param value new value
   * @return the parameter
   */
  public SystemParameter change(String key, String value) {
    SystemParameter p = parameters.get(key);
    if (!p.isSecurity()) {
      return parameters.update(key, value);
    }
    String by = currentUser.username();
    p.requestChange(value, by, clock.instant());
    repository.saveAndFlush(p);
    audit.record(
        ENTITY,
        key,
        AuditAction.SUBMIT,
        "Asked to change "
            + key
            + " from '"
            + p.getValue()
            + "' to '"
            + p.getPendingValue()
            + "', waiting for approval");
    events.publishEvent(
        new SecurityParameterChangeRequested(
            key, p.getDescription(), p.getValue(), p.getPendingValue(), by));
    return p;
  }

  /**
   * Approves the change that waits (not by its requester) and applies it.
   *
   * @param key key
   * @return the parameter
   */
  public SystemParameter approve(String key) {
    SystemParameter p = parameters.get(key);
    String requestedBy = p.getPendingBy();
    String value = p.approveChange(currentUser.username());
    repository.saveAndFlush(p);
    audit.record(
        ENTITY,
        key,
        AuditAction.AUTHORIZE,
        "Approved the change of " + key + " to '" + value + "' asked by " + requestedBy);
    return parameters.update(key, value);
  }

  /**
   * Rejects the change that waits (the requester may withdraw it too).
   *
   * @param key key
   * @return the parameter
   */
  public SystemParameter reject(String key) {
    SystemParameter p = parameters.get(key);
    String value = p.getPendingValue();
    String requestedBy = p.getPendingBy();
    p.rejectChange();
    audit.record(
        ENTITY,
        key,
        AuditAction.REJECT,
        "Rejected the change of " + key + " to '" + value + "' asked by " + requestedBy);
    return p;
  }

  /**
   * Security parameters whose change waits for approval.
   *
   * @return parameters
   */
  @Transactional(readOnly = true)
  public List<SystemParameter> pending() {
    return parameters.list().stream().filter(p -> p.getPendingValue() != null).toList();
  }
}
