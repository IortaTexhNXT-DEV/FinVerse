package com.iortatechnxt.brokerverse.security.api.dto;

import jakarta.validation.constraints.Size;

/**
 * "Forgot password?": the user ID to e-mail a link to, or the token of a link to check (UAM-NFR-37;
 * FR-UA-005).
 *
 * @param userId user ID (request a link)
 * @param token token of a link (check a link)
 */
public record PasswordResetLinkRequest(
    @Size(max = 50) String userId, @Size(max = 100) String token) {

  @Override
  public String toString() {
    return "PasswordResetLinkRequest[userId=" + userId + ", token=***]";
  }
}
