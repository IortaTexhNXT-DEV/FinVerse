package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.security.api.dto.SessionResponse;
import com.iortatechnxt.brokerverse.security.service.AuthSessionService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The session log for the System Administrator (UAM-NFR-35; FR-UA-004): sessions by user, the users
 * online (status Online on the Users screen, UQ13) and "End Session".
 */
@RestController
@RequestMapping("/api/v1/admin/sessions")
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class UserSessionController {

  private final AuthSessionService sessions;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param sessions session list
   * @param clock clock
   */
  public UserSessionController(AuthSessionService sessions, Clock clock) {
    this.sessions = sessions;
    this.clock = clock;
  }

  /**
   * Sessions, newest first.
   *
   * @param username user; all users when absent
   * @param open only the sessions open now
   * @param page page number
   * @param size page size
   * @return sessions
   */
  @GetMapping
  public PageResponse<SessionResponse> list(
      @RequestParam(required = false) String username,
      @RequestParam(defaultValue = "false") boolean open,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Instant now = clock.instant();
    return PageResponse.of(
        sessions.list(username, open, page, size), s -> SessionResponse.from(s, now));
  }

  /**
   * User names with an open session.
   *
   * @return user names
   */
  @GetMapping("/online")
  public List<String> online() {
    return sessions.online();
  }

  /**
   * Ends a session: its token is refused from now on.
   *
   * @param sessionId session (token) id
   * @return the ended session
   */
  @PostMapping("/{sessionId}/end")
  public SessionResponse end(@PathVariable String sessionId) {
    return SessionResponse.from(sessions.end(sessionId), clock.instant());
  }
}
