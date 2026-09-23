package com.iortatechnxt.finverse.payables.api.dto;

import com.iortatechnxt.finverse.payables.domain.PettyCashDisbursementValues;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Petty cash disbursement voucher request.
 *
 * @param date disbursement date
 * @param payee person paid
 * @param expenseAccountCode expense account
 * @param costCenter cost centre
 * @param description description
 * @param receiptRef receipt reference
 * @param amount amount
 */
public record DisbursementRequest(
    @NotNull LocalDate date,
    @NotBlank @Size(max = 120) String payee,
    @NotBlank @Size(max = 30) String expenseAccountCode,
    @Size(max = 20) String costCenter,
    @NotBlank @Size(max = 200) String description,
    @Size(max = 40) String receiptRef,
    @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {

  /**
   * Converts to domain values.
   *
   * @return values
   */
  public PettyCashDisbursementValues toValues() {
    return new PettyCashDisbursementValues(
        date, payee, expenseAccountCode, costCenter, description, receiptRef, amount);
  }
}
