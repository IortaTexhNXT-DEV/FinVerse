package com.iortatechnxt.brokerverse.security.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Password change request. Policy: 10+ characters with upper, lower, digit and symbol.
 *
 * @param currentPassword current password (ignored for administrator resets)
 * @param newPassword new password
 */
public record PasswordChangeRequest(
    String currentPassword,
    @NotBlank @Size(min = 10, max = 100) @Pattern(regexp = COMPLEXITY, message = COMPLEXITY_MESSAGE)
        String newPassword) {

  /** Complexity rule of a new password (UAM-NFR-31; FR-UA-005 R1). */
  public static final String COMPLEXITY = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$";

  /** Message of the complexity rule. */
  public static final String COMPLEXITY_MESSAGE =
      "must contain upper and lower case letters, a digit and a symbol";

  @Override
  public String toString() {
    return "PasswordChangeRequest[***]";
  }
}
