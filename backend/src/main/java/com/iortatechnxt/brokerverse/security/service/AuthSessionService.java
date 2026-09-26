package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import com.iortatechnxt.brokerverse.security.domain.UserSession;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The session list (UAM-NFR-35; FR-UA-004): the signed-in user's own sessions, every session and
 * the users online for the System Administrator, and the administrator's "End Session", which
 * revokes the token on every instance and ends the session (ADMIN_ENDED).
 */
@Service
@Transactional
public class AuthSessionService {

  private static final String ENTITY = "AppUser";

  private final UserSessionLog sessions;
  private final TokenRevocationStore revocations;
  private final CurrentUser currentUser;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param sessions session log
   * @param revocations token denylist
   * @param currentUser current user
   * @param audit audit trail
   */
  public AuthSessionService(
      UserSessionLog sessions,
      TokenRevocationStore revocations,
      CurrentUser currentUser,
      AuditTrailService audit) {
    this.sessions = sessions;
    this.revocations = revocations;
    this.currentUser = currentUser;
    this.audit = audit;
  }

  /**
   * The signed-in user's sessions, newest first.
   *
   * @param page page number
   * @param size page size
   * @return sessions
   */
  @Transactional(readOnly = true)
  public Page<UserSession> mine(int page, int size) {
    return sessions.list(currentUser.username(), false, page, size);
  }

  /**
   * Sessions of a user or of everybody, newest first.
   *
   * @param username user; null or blank for every user
   * @param openOnly only the sessions open now
   * @param page page number
   * @param size page size
   * @return sessions
   */
  @Transactional(readOnly = true)
  public Page<UserSession> list(String username, boolean openOnly, int page, int size) {
    String user = username == null || username.isBlank() ? null : username.trim();
    return sessions.list(user, openOnly, page, size);
  }

  /**
   * User names with an open session (status Online, UQ13).
   *
   * @return user names
   */
  @Transactional(readOnly = true)
  public List<String> online() {
    return sessions.onlineUsernames();
  }

  /**
   * Ends a session as an administrator: its token is refused from now on, on every instance.
   *
   * @param sessionId session (token) id
   * @return the ended session
   */
  public UserSession end(String sessionId) {
    UserSession session =
        sessions
            .find(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
    if (session.getEndedAt() != null) {
      throw new BusinessRuleException("SESSION_ALREADY_ENDED", "The session has already ended");
    }
    revocations.revoke(session.getSessionId(), session.getUsername(), session.getExpiresAt());
    sessions.end(sessionId, SessionEndReason.ADMIN_ENDED);
    audit.record(
        ENTITY,
        session.getUsername(),
        AuditAction.LOGOUT,
        "Session ended by administrator " + currentUser.username());
    return session;
  }
}
