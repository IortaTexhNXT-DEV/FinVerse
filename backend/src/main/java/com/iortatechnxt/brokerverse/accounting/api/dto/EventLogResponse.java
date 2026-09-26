package com.iortatechnxt.brokerverse.accounting.api.dto;

import com.iortatechnxt.brokerverse.accounting.domain.AccountingEventLog;
import com.iortatechnxt.brokerverse.accounting.domain.EventStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Event register entry.
 *
 * @param id id
 * @param eventType event type
 * @param sourceModule source module
 * @param sourceReference source key
 * @param reference business reference
 * @param valueDate value date
 * @param status outcome
 * @param ruleId rule applied
 * @param batchNo journal posted
 * @param errorMessage failure reason
 * @param amounts amounts text
 * @param processedAt time
 * @param processedBy user
 */
public record EventLogResponse(
    Long id,
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
    String processedBy) {

  /**
   * Maps an entity.
   *
   * @param e log
   * @return response
   */
  public static EventLogResponse from(AccountingEventLog e) {
    return new EventLogResponse(
        e.getId(),
        e.getEventType(),
        e.getSourceModule(),
        e.getSourceReference(),
        e.getReference(),
        e.getValueDate(),
        e.getStatus(),
        e.getRuleId(),
        e.getBatchNo(),
        e.getErrorMessage(),
        e.getAmounts(),
        e.getProcessedAt(),
        e.getProcessedBy());
  }
}
