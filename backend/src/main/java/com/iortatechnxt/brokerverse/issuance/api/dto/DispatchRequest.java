package com.iortatechnxt.brokerverse.issuance.api.dto;

import com.iortatechnxt.brokerverse.issuance.service.EpolicyDispatchService.DispatchEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Sends one e-policy to the client, encrypted (BRNB.077).
 *
 * @param to recipients
 * @param cc copy
 * @param subject subject
 * @param body body
 * @param passwordHint how the password is built
 */
public record DispatchRequest(
    @NotEmpty @Size(max = 20) List<String> to,
    @Size(max = 20) List<String> cc,
    @NotBlank @Size(max = 300) String subject,
    @NotBlank @Size(max = 10000) String body,
    @Size(max = 300) String passwordHint) {

  /**
   * The service e-mail.
   *
   * @return e-mail
   */
  public DispatchEmail toEmail() {
    return new DispatchEmail(to, cc, subject, body, passwordHint);
  }
}
