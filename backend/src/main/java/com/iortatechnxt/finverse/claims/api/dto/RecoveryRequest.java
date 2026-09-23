package com.iortatechnxt.finverse.claims.api.dto;

import com.iortatechnxt.finverse.claims.domain.RecoveryType;
import com.iortatechnxt.finverse.claims.service.RecoveryCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * New recovery, at 100 %.
 *
 * @param recoveryType salvage or subrogation
 * @param fromPartyCode payer party, optional
 * @param bankAccountCode GL bank account that received the money
 * @param amount amount received
 * @param narration narration
 */
public record RecoveryRequest(
    @NotNull RecoveryType recoveryType,
    @Size(max = 30) String fromPartyCode,
    @NotBlank @Size(max = 20) String bankAccountCode,
    @NotNull @Positive BigDecimal amount,
    @NotBlank @Size(max = 300) String narration) {

  /**
   * Service command.
   *
   * @return command
   */
  public RecoveryCommand toCommand() {
    return new RecoveryCommand(recoveryType, fromPartyCode, bankAccountCode, amount, narration);
  }
}
