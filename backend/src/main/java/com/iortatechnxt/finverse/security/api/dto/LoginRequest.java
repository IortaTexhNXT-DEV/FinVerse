package com.iortatechnxt.finverse.security.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login request.
 *
 * @param username user name
 * @param password password
 */
public record LoginRequest(
    @NotBlank @Size(max = 50) String username, @NotBlank @Size(max = 100) String password) {

  @Override
  public String toString() {
    return "LoginRequest[username=" + username + ", password=***]";
  }
}
