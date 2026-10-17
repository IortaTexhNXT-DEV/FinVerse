package com.iortatechnxt.brokerverse.journal.domain;

import java.time.LocalDate;

/**
 * Header values of a journal batch.
 *
 * @param companyId company
 * @param branchId originating branch
 * @param journalType type
 * @param transactionDate entry date
 * @param valueDate accounting (value) date that determines the period
 * @param currency header currency (default for lines)
 * @param narration narration
 * @param reference external reference
 * @param sourceModule originating module for system journals
 * @param sourceReference idempotency key of the source business event
 * @param reversalOfId batch reversed by this one, if any
 */
public record JournalHeader(
    Long companyId,
    Long branchId,
    JournalType journalType,
    LocalDate transactionDate,
    LocalDate valueDate,
    String currency,
    String narration,
    String reference,
    String sourceModule,
    String sourceReference,
    Long reversalOfId) {}
