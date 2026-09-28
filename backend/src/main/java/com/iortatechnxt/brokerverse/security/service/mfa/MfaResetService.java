package com.iortatechnxt.brokerverse.security.service.mfa;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.security.UserDisplayNames;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.MfaResetRequest;
import com.iortatechnxt.brokerverse.security.domain.MfaResetRequestRepository;
import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import com.iortatechnxt.brokerverse.security.domain.UserMfa;
import com.iortatechnxt.brokerverse.security.service.UserSessionLog;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administrator reset of a user's second factor under four eyes: a holder of {@value #REQUEST} asks
 * for it with a reason; a holder of {@value #APPROVE} other than the requester (and other than the
 * user concerned) approves it, which removes the user's authenticator app, recovery codes and
 * remembered devices and ends the user's sessions; the user enrols again at the next sign-in. The
 * approver may reject with a reason, the requester may withdraw. Every step is audited.
 */
@Service
@Transactional
public class MfaResetService {

  /** Permission of the requesters. */
  public static final String REQUEST = "MFA_RESET";

  /** Permission of the approvers. */
  public static final String APPROVE = "MFA_RESET_APPROVE";

  private static final String ENTITY = "AppUser";

  private final MfaResetRequestRepository requests;
  private final AppUserRepository users;
  private final MfaService mfa;
  private final UserSessionLog sessions;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final UserDisplayNames names;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests reset requests
   * @param users users
   * @param mfa second factor
   * @param sessions session log (the user's sessions end)
   * @param audit audit trail
   * @param currentUser current user
   * @param names display names in the audit texts
   * @param events events (notices)
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the four-eyes reset
  public MfaResetService(
      MfaResetRequestRepository requests,
      AppUserRepository users,
      MfaService mfa,
      UserSessionLog sessions,
      AuditTrailService audit,
      CurrentUser currentUser,
      UserDisplayNames names,
      ApplicationEventPublisher events,
      Clock clock) {
    this.requests = requests;
    this.users = users;
    this.mfa = mfa;
    this.sessions = sessions;
    this.audit = audit;
    this.currentUser = currentUser;
    this.names = names;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Asks for the reset of a user's second factor.
   *
   * @param username user
   * @param reason why (mandatory)
   * @return the request
   */
  public MfaResetRequest request(String username, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("REASON_REQUIRED", "Give the reason for the reset");
    }
    AppUser user =
        users
            .findByUsernameIgnoreCase(username == null ? "" : username.trim())
            .orElseThrow(() -> new ResourceNotFoundException("User", username));
    if (!mfa.enrolled(user.getUsername())) {
      throw new BusinessRuleException(
          "MFA_NOT_ENROLLED", "This user has no authenticator app to reset");
    }
    if (requests
        .findFirstByUsernameIgnoreCaseAndStatus(user.getUsername(), MfaResetRequest.PENDING)
        .isPresent()) {
      throw new BusinessRuleException(
          "MFA_RESET_PENDING", "A reset of this user's second factor already waits for approval");
    }
    String by = currentUser.username();
    MfaResetRequest request =
        requests.save(new MfaResetRequest(user.getUsername(), reason.strip(), by, clock.instant()));
    audit.record(
        ENTITY,
        user.getUsername(),
        AuditAction.SUBMIT,
        "Requested the reset of the second factor, waiting for approval. Reason: "
            + reason.strip());
    events.publishEvent(
        new MfaResetRequested(request.getId(), user.getUsername(), by, reason.strip()));
    return request;
  }

  /**
   * Approves a reset and applies it (not by the requester, not by the user concerned).
   *
   * @param id request
   * @return the request
   */
  public MfaResetRequest approve(Long id) {
    MfaResetRequest request = pending(id);
    String by = currentUser.username();
    if (CurrentUser.sameUser(by, request.getRequestedBy())) {
      throw new BusinessRuleException(
          "FOUR_EYES", "Another administrator must approve a reset you requested");
    }
    if (CurrentUser.sameUser(by, request.getUsername())) {
      throw new BusinessRuleException(
          "FOUR_EYES", "You cannot approve the reset of your own second factor");
    }
    request.decide(MfaResetRequest.APPROVED, by, clock.instant(), null);
    mfa.removeAll(request.getUsername());
    sessions.endAll(request.getUsername(), SessionEndReason.ADMIN_ENDED);
    audit.record(
        ENTITY,
        request.getUsername(),
        AuditAction.AUTHORIZE,
        "Approved the reset of the second factor requested by "
            + names.displayName(request.getRequestedBy())
            + "; the user enrols again at the next sign-in");
    events.publishEvent(new MfaResetApplied(request.getUsername()));
    return request;
  }

  /**
   * Rejects a reset with a reason, or withdraws it (the requester, no reason needed).
   *
   * @param id request
   * @param reason why it is rejected
   * @return the request
   */
  public MfaResetRequest reject(Long id, String reason) {
    MfaResetRequest request = pending(id);
    String by = currentUser.username();
    boolean withdrawn = CurrentUser.sameUser(by, request.getRequestedBy());
    if (!withdrawn && !currentUser.hasAuthority(APPROVE)) {
      throw new BusinessRuleException(
          "ACCESS_DENIED", "Only an approver or the requester may close this request");
    }
    if (!withdrawn && (reason == null || reason.isBlank())) {
      throw new BusinessRuleException("REASON_REQUIRED", "Give the reason for the rejection");
    }
    String note = reason == null || reason.isBlank() ? null : reason.strip();
    request.decide(
        withdrawn ? MfaResetRequest.WITHDRAWN : MfaResetRequest.REJECTED,
        by,
        clock.instant(),
        note);
    audit.record(
        ENTITY,
        request.getUsername(),
        AuditAction.REJECT,
        (withdrawn ? "Withdrew" : "Rejected")
            + " the reset of the second factor requested by "
            + names.displayName(request.getRequestedBy())
            + (note == null ? "" : ". Reason: " + note));
    return request;
  }

  /**
   * Reset requests, newest first.
   *
   * @param status PENDING, APPROVED, REJECTED or WITHDRAWN; null for all
   * @return up to 200 requests
   */
  @Transactional(readOnly = true)
  public List<MfaResetRequest> list(String status) {
    return status == null || status.isBlank()
        ? requests.findTop200ByOrderByRequestedAtDesc()
        : requests.findTop200ByStatusOrderByRequestedAtDesc(status.trim());
  }

  /**
   * The users with their second factor, for the administrators' screen.
   *
   * @return users ordered by user name
   */
  @Transactional(readOnly = true)
  public List<UserSecondFactor> users() {
    Set<String> pending =
        requests.findTop200ByStatusOrderByRequestedAtDesc(MfaResetRequest.PENDING).stream()
            .map(r -> r.getUsername().toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
    Map<String, UserMfa> enrolled = mfa.activeEnrolments();
    return users.findAll(Sort.by("username")).stream()
        .map(
            u -> {
              String key = u.getUsername().toLowerCase(Locale.ROOT);
              UserMfa app = enrolled.get(key);
              return new UserSecondFactor(
                  u.getUsername(),
                  u.getFullName(),
                  u.isEnabled(),
                  MfaPolicy.privileged(u),
                  app != null,
                  app == null ? null : app.getActivatedAt(),
                  app == null ? null : app.getLastUsedAt(),
                  pending.contains(key));
            })
        .toList();
  }

  private MfaResetRequest pending(Long id) {
    MfaResetRequest request =
        requests
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Second factor reset request", id));
    if (!request.isPending()) {
      throw new BusinessRuleException("REQUEST_CLOSED", "This request is already closed");
    }
    return request;
  }

  /**
   * A user and the state of the second factor.
   *
   * @param username user name
   * @param fullName display name
   * @param enabled whether the account is active
   * @param privileged whether the user holds a privileged role
   * @param enrolled whether an authenticator app is active
   * @param enrolledAt when it was enrolled
   * @param lastUsedAt when a code was last accepted
   * @param resetPending whether a reset waits for approval
   */
  public record UserSecondFactor(
      String username,
      String fullName,
      boolean enabled,
      boolean privileged,
      boolean enrolled,
      Instant enrolledAt,
      Instant lastUsedAt,
      boolean resetPending) {}
}
