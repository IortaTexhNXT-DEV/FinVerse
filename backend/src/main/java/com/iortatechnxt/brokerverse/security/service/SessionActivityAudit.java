package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records the inactivity of a signed-in user in the audit trail (BDOI FRS FRUM.001.03): the web
 * client reports that the inactivity warning was shown, and the entry carries the action Inactivity
 * with the minutes set in SESSION_IDLE_WARNING_MINUTES.
 */
@Service
public class SessionActivityAudit {

  private static final String ENTITY = "AppUser";
  private static final int DEFAULT_IDLE_WARNING_MINUTES = 15;

  private final AuditTrailService audit;
  private final SystemParameterService parameters;
  private final CurrentUser currentUser;

  /**
   * Creates the service.
   *
   * @param audit audit trail
   * @param parameters business parameters (inactivity time)
   * @param currentUser signed-in user
   */
  public SessionActivityAudit(
      AuditTrailService audit, SystemParameterService parameters, CurrentUser currentUser) {
    this.audit = audit;
    this.parameters = parameters;
    this.currentUser = currentUser;
  }

  /** Records that the inactivity warning was shown to the signed-in user. */
  @Transactional
  public void inactivityWarningShown() {
    String username = currentUser.username();
    audit.record(
        ENTITY,
        username,
        AuditAction.INACTIVITY,
        "Inactive for "
            + parameters.intValue(
                SystemParameterService.SESSION_IDLE_WARNING_MINUTES, DEFAULT_IDLE_WARNING_MINUTES)
            + " minutes; the inactivity warning was shown");
  }
}
