package com.iortatechnxt.finverse.payables.domain;

import java.math.BigDecimal;

/**
 * Entered values of a supplier invoice line.
 *
 * @param expenseAccountCode GL account debited (expense, prepayment or asset)
 * @param costCenter cost centre
 * @param description description
 * @param netAmount amount net of VAT
 */
public record InvoiceLineValues(
    String expenseAccountCode, String costCenter, String description, BigDecimal netAmount) {}
