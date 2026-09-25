package com.iortatechnxt.brokerverse.receivables.api.dto;

import com.iortatechnxt.brokerverse.receivables.domain.StatementLayoutValues;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Statement layout of a bank account (FRBS 3.3.1) and its matching rule (FRBS 3.3.2).
 *
 * @param companyId company
 * @param bankAccountCode GL bank account
 * @param name layout name
 * @param dateColumn date column
 * @param descriptionColumn description column
 * @param referenceColumn reference / cheque number column
 * @param debitColumn withdrawals column
 * @param creditColumn deposits column
 * @param amountColumn signed amount column
 * @param balanceColumn running balance column
 * @param datePattern date pattern
 * @param chequeNumberFirst match cheque number and amount first
 */
public record StatementLayoutRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 30) String bankAccountCode,
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 60) String dateColumn,
    @Size(max = 60) String descriptionColumn,
    @Size(max = 60) String referenceColumn,
    @Size(max = 60) String debitColumn,
    @Size(max = 60) String creditColumn,
    @Size(max = 60) String amountColumn,
    @Size(max = 60) String balanceColumn,
    @Size(max = 20) String datePattern,
    boolean chequeNumberFirst) {

  /**
   * The mapping.
   *
   * @return values
   */
  public StatementLayoutValues values() {
    return new StatementLayoutValues(
        name.trim(),
        dateColumn.trim(),
        descriptionColumn,
        referenceColumn,
        debitColumn,
        creditColumn,
        amountColumn,
        balanceColumn,
        datePattern);
  }
}
