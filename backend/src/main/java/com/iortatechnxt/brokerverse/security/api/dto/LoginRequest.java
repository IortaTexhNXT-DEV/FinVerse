package com.iortatechnxt.brokerverse.security.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login request.
 *
 * @param username user name
 * @param password password
 * @param deviceToken token of a device remembered for the second factor, may be null
 */
public record LoginRequest(
    @NotBlank @Size(max = 50) String username,
    @NotBlank @Size(max = 100) String password,
    @Size(max = 100) String deviceToken) {

  @Override
  public String toString() {
    return "LoginRequest[username=" + username + ", password=***]";
  }
}
