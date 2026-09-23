package com.iortatechnxt.finverse.alert.domain;

import java.math.BigDecimal;

/**
 * What an exception condition found and where.
 *
 * @param companyId company (null when not company specific)
 * @param branchId branch (optional)
 * @param entityType affected record type, e.g. "JournalBatch"
 * @param entityId affected record key, e.g. the batch number
 * @param message human readable description
 * @param amount amount involved (optional)
 * @param dedupKey condition identity: while an alert with this key is live no new one is raised
 */
public record AlertFacts(
    Long companyId,
    Long branchId,
    String entityType,
    String entityId,
    String message,
    BigDecimal amount,
    String dedupKey) {}
