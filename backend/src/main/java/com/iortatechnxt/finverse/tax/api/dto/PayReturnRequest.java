package com.iortatechnxt.finverse.tax.api.dto;

import com.iortatechnxt.finverse.tax.domain.RemittanceFacts;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request to pay a filed return.
 *
 * @param paidOn payment date
 * @param bankAccountCode company bank account (payables bank account code); may be blank for a
 *     return with nothing to pay
 * @param reference bank / eFPS payment reference
 */
public record PayReturnRequest(
    @NotNull LocalDate paidOn,
    @Size(max = 20) String bankAccountCode,
    @NotBlank @Size(max = 60) String reference) {

  /**
   * Converts to the domain value.
   *
   * @return payment facts
   */
  public RemittanceFacts toFacts() {
    return new RemittanceFacts(paidOn, bankAccountCode, reference);
  }
}
