package com.iortatechnxt.brokerverse.placement.api.dto;

import com.iortatechnxt.brokerverse.placement.service.PlacementSlipService.SlipEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * The e-mail sending a slip to the insurer (BRNB.071).
 *
 * @param to recipients
 * @param cc copy
 * @param subject subject
 * @param body body
 * @param protect password-protect the files, password in a separate e-mail
 */
public record SlipEmailRequest(
    @NotEmpty @Size(max = 20) List<String> to,
    @Size(max = 20) List<String> cc,
    @NotBlank @Size(max = 300) String subject,
    @NotBlank @Size(max = 10000) String body,
    boolean protect) {

  /**
   * The service e-mail.
   *
   * @return e-mail
   */
  public SlipEmail toEmail() {
    return new SlipEmail(to, cc, subject, body, protect);
  }
}
