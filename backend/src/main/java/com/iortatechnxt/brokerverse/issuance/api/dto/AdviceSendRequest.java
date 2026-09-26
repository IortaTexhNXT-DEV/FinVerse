package com.iortatechnxt.brokerverse.issuance.api.dto;

import com.iortatechnxt.brokerverse.issuance.service.AdviceDispatchService.AdviceEmail;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Sends Insurance Advices, protected (BRNB.060/035).
 *
 * @param adviceIds advices
 * @param to recipients
 * @param cc copy
 * @param passwordHint how the password is built
 */
public record AdviceSendRequest(
    @NotEmpty @Size(max = 50) List<@NotNull Long> adviceIds,
    @NotEmpty @Size(max = 20) List<String> to,
    @Size(max = 20) List<String> cc,
    @Size(max = 300) String passwordHint) {

  /**
   * The service request.
   *
   * @return e-mail
   */
  public AdviceEmail toEmail() {
    return new AdviceEmail(adviceIds, to, cc, passwordHint);
  }
}
