package com.iortatechnxt.finverse.subledger.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Values of a new open item.
 *
 * @param companyId company
 * @param branchId branch
 * @param partyId party id
 * @param partyCode party code
 * @param direction DEBIT (party owes company) or CREDIT (company owes party)
 * @param documentType document type, e.g. DEBIT_NOTE, RECEIPT, SUPPLIER_INVOICE
 * @param documentNo document number
 * @param documentDate document date
 * @param dueDate due date (drives ageing)
 * @param currency currency
 * @param amount positive amount in currency
 * @param baseAmount positive amount in base currency
 * @param sourceModule originating module
 * @param sourceReference originating record key
 * @param journalBatchNo GL batch that recorded the document
 * @param narration narration
 */
public record OpenItemValues(
    Long companyId,
    Long branchId,
    Long partyId,
    String partyCode,
    ItemDirection direction,
    String documentType,
    String documentNo,
    LocalDate documentDate,
    LocalDate dueDate,
    String currency,
    BigDecimal amount,
    BigDecimal baseAmount,
    String sourceModule,
    String sourceReference,
    String journalBatchNo,
    String narration) {}
