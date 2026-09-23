package com.iortatechnxt.finverse.consolidation.api.dto;

import com.iortatechnxt.finverse.consolidation.domain.IntercompanyTransactionType;
import com.iortatechnxt.finverse.consolidation.domain.IntercompanyValues;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Inter-company transaction entry; posts one journal in each company.
 *
 * @param type CHARGE (creates the balances) or SETTLEMENT (reduces them)
 * @param creditorCompanyId company owed the money
 * @param debtorCompanyId company owing the money
 * @param valueDate value date of both journals
 * @param currency transaction currency
 * @param amount amount
 * @param creditorAccount creditor's counter account (e.g. income or bank)
 * @param debtorAccount debtor's counter account (e.g. expense or bank)
 * @param narration narration
 * @param costCenter optional cost centre for the counter accounts
 * @param businessLine optional line of business for the counter accounts
 */
public record IntercompanyTransactionRequest(
    @NotNull IntercompanyTransactionType type,
    @NotNull Long creditorCompanyId,
    @NotNull Long debtorCompanyId,
    @NotNull LocalDate valueDate,
    @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
    @NotBlank @Size(max = 30) String creditorAccount,
    @NotBlank @Size(max = 30) String debtorAccount,
    @NotBlank @Size(max = 250) String narration,
    @Size(max = 20) String costCenter,
    @Size(max = 20) String businessLine) {

  /**
   * Business values.
   *
   * @return values
   */
  public IntercompanyValues values() {
    return new IntercompanyValues(
        type,
        creditorCompanyId,
        debtorCompanyId,
        valueDate,
        currency,
        amount,
        creditorAccount.trim(),
        debtorAccount.trim(),
        narration.trim());
  }
}
