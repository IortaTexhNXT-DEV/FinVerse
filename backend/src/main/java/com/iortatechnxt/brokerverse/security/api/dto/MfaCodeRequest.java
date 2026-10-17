package com.iortatechnxt.brokerverse.security.api.dto;

import jakarta.validation.constraints.Size;

/**
 * A code of the second factor, with the challenge of the first sign-in step when the user is not
 * signed in yet.
 *
 * @param challenge challenge token of the first step (sign-in only)
 * @param code code of the authenticator app or a recovery code
 * @param rememberDevice whether to remember this device (only when the parameter allows it)
 */
public record MfaCodeRequest(
    @Size(max = 2000) String challenge, @Size(max = 20) String code, boolean rememberDevice) {

  @Override
  public String toString() {
    return "MfaCodeRequest[code=***, rememberDevice=" + rememberDevice + "]";
  }
}
