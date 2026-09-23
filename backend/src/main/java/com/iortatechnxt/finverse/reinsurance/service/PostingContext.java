package com.iortatechnxt.finverse.reinsurance.service;

import java.time.LocalDate;

/**
 * Where and when a reinsurance posting is made.
 *
 * @param companyId company
 * @param branchId branch that owns the underlying policy or claim
 * @param date accounting date
 * @param businessLine line of business (mandatory on reinsurance income and expense accounts)
 * @param documentNo business document shown on the ledger and the open item (cession, placement,
 *     claim or statement number)
 * @param narration narration
 */
public record PostingContext(
    Long companyId,
    Long branchId,
    LocalDate date,
    String businessLine,
    String documentNo,
    String narration) {}
