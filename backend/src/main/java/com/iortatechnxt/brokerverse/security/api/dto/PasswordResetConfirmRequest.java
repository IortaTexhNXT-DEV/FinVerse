package com.iortatechnxt.brokerverse.security.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A new password set with a "Forgot password?" link (UAM-NFR-37; FR-UA-005). The policy is that of
 * {@link PasswordChangeRequest}.
 *
 * @param token token of the link
 * @param newPassword new password
 */
public record PasswordResetConfirmRequest(
    @NotBlank @Size(max = 100) String token,
    @NotBlank
        @Size(min = 10, max = 100)
        @Pattern(
            regexp = PasswordChangeRequest.COMPLEXITY,
            message = PasswordChangeRequest.COMPLEXITY_MESSAGE)
        String newPassword) {

  @Override
  public String toString() {
    return "PasswordResetConfirmRequest[***]";
  }
}
