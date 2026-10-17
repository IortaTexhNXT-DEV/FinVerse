package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.security.service.SessionActivityAudit;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * What the web client reports about the signed-in session: the inactivity warning shown to the
 * user, recorded in the audit trail (BDOI FRS FRUM.001.03).
 */
@RestController
@RequestMapping("/api/v1/auth/session")
@PreAuthorize("isAuthenticated()")
public class SessionActivityController {

  private final SessionActivityAudit activity;

  /**
   * Creates the controller.
   *
   * @param activity audit of the session activity
   */
  public SessionActivityController(SessionActivityAudit activity) {
    this.activity = activity;
  }

  /** Records that the inactivity warning was shown to the signed-in user. */
  @PostMapping("/inactivity")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void inactivity() {
    activity.inactivityWarningShown();
  }
}
