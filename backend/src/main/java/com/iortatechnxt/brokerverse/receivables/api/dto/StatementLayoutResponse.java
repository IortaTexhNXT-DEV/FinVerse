package com.iortatechnxt.brokerverse.receivables.api.dto;

import com.iortatechnxt.brokerverse.receivables.domain.StatementLayout;
import com.iortatechnxt.brokerverse.receivables.domain.StatementLayoutValues;

/**
 * Statement layout view.
 *
 * @param id id
 * @param bankAccountCode GL bank account
 * @param name layout name
 * @param dateColumn date column
 * @param descriptionColumn description column
 * @param referenceColumn reference column
 * @param debitColumn withdrawals column
 * @param creditColumn deposits column
 * @param amountColumn signed amount column
 * @param balanceColumn balance column
 * @param datePattern date pattern
 * @param chequeNumberFirst cheque number and amount matched first
 */
public record StatementLayoutResponse(
    Long id,
    String bankAccountCode,
    String name,
    String dateColumn,
    String descriptionColumn,
    String referenceColumn,
    String debitColumn,
    String creditColumn,
    String amountColumn,
    String balanceColumn,
    String datePattern,
    boolean chequeNumberFirst) {

  /**
   * Maps an entity.
   *
   * @param l layout
   * @param chequeNumberFirst matching rule
   * @return response
   */
  public static StatementLayoutResponse from(StatementLayout l, boolean chequeNumberFirst) {
    StatementLayoutValues v = l.values();
    return new StatementLayoutResponse(
        l.getId(),
        l.getBankAccountCode(),
        v.name(),
        v.dateColumn(),
        v.descriptionColumn(),
        v.referenceColumn(),
        v.debitColumn(),
        v.creditColumn(),
        v.amountColumn(),
        v.balanceColumn(),
        v.datePattern(),
        chequeNumberFirst);
  }
}
