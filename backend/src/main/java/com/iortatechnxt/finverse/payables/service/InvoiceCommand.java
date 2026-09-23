package com.iortatechnxt.finverse.payables.service;

import com.iortatechnxt.finverse.payables.domain.InvoiceLineValues;
import java.time.LocalDate;
import java.util.List;

/**
 * Values to create or update a supplier invoice.
 *
 * @param companyId company
 * @param branchId branch
 * @param partyCode supplier code (immutable after creation)
 * @param supplierInvoiceNo supplier's invoice number
 * @param invoiceDate invoice date
 * @param dueDate due date, null = invoice date + supplier credit days
 * @param currency currency, null = supplier default currency
 * @param vatApplicable whether 12 % input VAT applies
 * @param narration narration
 * @param lines expense lines
 */
public record InvoiceCommand(
    Long companyId,
    Long branchId,
    String partyCode,
    String supplierInvoiceNo,
    LocalDate invoiceDate,
    LocalDate dueDate,
    String currency,
    boolean vatApplicable,
    String narration,
    List<InvoiceLineValues> lines) {

  /** Canonical constructor copying the lines. */
  public InvoiceCommand {
    lines = List.copyOf(lines);
  }
}
