package com.iortatechnxt.finverse.tax.service;

import java.math.BigDecimal;

/**
 * Reconciliation of a worksheet total with the general ledger: the posted movement of the tax
 * account in the period (tax remittance journals and year-end closing excluded) against the total
 * of the sub-ledger documents. A difference points to manual journals, documents of other modules
 * or timing (documents approved in one period and posted in another).
 *
 * @param accountCode GL account
 * @param description what is reconciled, e.g. "Output VAT"
 * @param perDocuments total of the worksheet documents
 * @param perLedger posted movement, presented on the account's natural side
 */
public record LedgerControl(
    String accountCode, String description, BigDecimal perDocuments, BigDecimal perLedger) {

  /**
   * Unreconciled difference.
   *
   * @return ledger − documents
   */
  public BigDecimal difference() {
    return perLedger.subtract(perDocuments);
  }
}
