package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.security.domain.MfaResetRequest;
import com.iortatechnxt.brokerverse.security.service.mfa.MfaResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administration of the second factor: the users and their authenticator app, and the reset of a
 * user's app under four eyes (MFA_RESET requests, MFA_RESET_APPROVE approves, never the requester).
 */
@RestController
@RequestMapping("/api/v1/admin/mfa")
public class MfaAdminController {

  private static final String READERS = "hasAnyAuthority('MFA_RESET', 'MFA_RESET_APPROVE')";

  private final MfaResetService resets;

  /**
   * Creates the controller.
   *
   * @param resets reset requests
   */
  public MfaAdminController(MfaResetService resets) {
    this.resets = resets;
  }

  /**
   * The users with the state of their second factor.
   *
   * @return users
   */
  @GetMapping("/users")
  @PreAuthorize(READERS)
  public List<MfaResetService.UserSecondFactor> users() {
    return resets.users();
  }

  /**
   * Reset requests, newest first.
   *
   * @param status PENDING, APPROVED, REJECTED or WITHDRAWN; all when absent
   * @return requests
   */
  @GetMapping("/reset-requests")
  @PreAuthorize(READERS)
  public List<ResetResponse> requests(@RequestParam(required = false) String status) {
    return resets.list(status).stream().map(ResetResponse::from).toList();
  }

  /**
   * Asks for the reset of a user's second factor.
   *
   * @param request user and reason
   * @return the request
   */
  @PostMapping("/reset-requests")
  @PreAuthorize("hasAuthority('MFA_RESET')")
  @ResponseStatus(HttpStatus.CREATED)
  public ResetResponse request(@Valid @RequestBody NewReset request) {
    return ResetResponse.from(resets.request(request.username(), request.reason()));
  }

  /**
   * Approves a reset (not the requester) and applies it.
   *
   * @param id request
   * @return the request
   */
  @PostMapping("/reset-requests/{id}/approve")
  @PreAuthorize("hasAuthority('MFA_RESET_APPROVE')")
  public ResetResponse approve(@PathVariable Long id) {
    return ResetResponse.from(resets.approve(id));
  }

  /**
   * Rejects a reset with a reason, or withdraws it (the requester).
   *
   * @param id request
   * @param body the reason
   * @return the request
   */
  @PostMapping("/reset-requests/{id}/reject")
  @PreAuthorize(READERS)
  public ResetResponse reject(@PathVariable Long id, @Valid @RequestBody Decision body) {
    return ResetResponse.from(resets.reject(id, body.reason()));
  }

  /**
   * A new reset request.
   *
   * @param username user
   * @param reason why
   */
  public record NewReset(
      @NotBlank @Size(max = 50) String username, @NotBlank @Size(max = 500) String reason) {}

  /**
   * A decision.
   *
   * @param reason reason of a rejection
   */
  public record Decision(@Size(max = 500) String reason) {}

  /**
   * A reset request.
   *
   * @param id id
   * @param username user
   * @param reason why
   * @param status PENDING, APPROVED, REJECTED or WITHDRAWN
   * @param requestedBy requester
   * @param requestedAt time
   * @param decidedBy approver or rejecter
   * @param decidedAt time of the decision
   * @param decisionNote reason of a rejection
   */
  public record ResetResponse(
      Long id,
      String username,
      String reason,
      String status,
      String requestedBy,
      Instant requestedAt,
      String decidedBy,
      Instant decidedAt,
      String decisionNote) {

    static ResetResponse from(MfaResetRequest r) {
      return new ResetResponse(
          r.getId(),
          r.getUsername(),
          r.getReason(),
          r.getStatus(),
          r.getRequestedBy(),
          r.getRequestedAt(),
          r.getDecidedBy(),
          r.getDecidedAt(),
          r.getDecisionNote());
    }
  }
}
