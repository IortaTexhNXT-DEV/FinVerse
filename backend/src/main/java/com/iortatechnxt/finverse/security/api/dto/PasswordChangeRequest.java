package com.iortatechnxt.finverse.security.api.dto;

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
    @NotBlank
        @Size(min = 10, max = 100)
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
            message = "must contain upper and lower case letters, a digit and a symbol")
        String newPassword) {

  @Override
  public String toString() {
    return "PasswordChangeRequest[***]";
  }
}
