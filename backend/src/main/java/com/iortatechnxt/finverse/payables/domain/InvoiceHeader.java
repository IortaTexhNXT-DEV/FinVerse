package com.iortatechnxt.finverse.payables.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Header values of a supplier invoice.
 *
 * @param companyId company
 * @param branchId branch that books the expense
 * @param partyId supplier party id
 * @param partyCode supplier party code
 * @param supplierInvoiceNo supplier's own invoice number
 * @param invoiceDate invoice (accounting) date
 * @param dueDate due date
 * @param currency invoice currency
 * @param vatApplicable whether 12 % input VAT applies
 * @param whtRate expanded withholding tax rate in percent
 * @param narration narration
 */
public record InvoiceHeader(
    Long companyId,
    Long branchId,
    Long partyId,
    String partyCode,
    String supplierInvoiceNo,
    LocalDate invoiceDate,
    LocalDate dueDate,
    String currency,
    boolean vatApplicable,
    BigDecimal whtRate,
    String narration) {}
