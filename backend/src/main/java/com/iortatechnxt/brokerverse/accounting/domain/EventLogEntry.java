package com.iortatechnxt.brokerverse.accounting.domain;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Values of an event log record.
 *
 * @param companyId company
 * @param eventType event type
 * @param sourceModule source module
 * @param sourceReference idempotency key
 * @param reference business reference
 * @param valueDate value date
 * @param status outcome
 * @param ruleId rule applied
 * @param batchNo journal batch posted
 * @param errorMessage failure reason
 * @param amounts amount components (text)
 * @param processedAt timestamp
 * @param processedBy user
 */
public record EventLogEntry(
    Long companyId,
    String eventType,
    String sourceModule,
    String sourceReference,
    String reference,
    LocalDate valueDate,
    EventStatus status,
    Long ruleId,
    String batchNo,
    String errorMessage,
    String amounts,
    Instant processedAt,
    String processedBy) {}
