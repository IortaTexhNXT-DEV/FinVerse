package com.iortatechnxt.brokerverse.closing.api.dto;

import com.iortatechnxt.brokerverse.closing.domain.PeriodCloseSchedule;
import java.time.Instant;

/**
 * Scheduled close view.
 *
 * @param id id
 * @param periodId period
 * @param periodName period name
 * @param scheduledAt scheduled time
 * @param status SCHEDULED, COMPLETED, FAILED or CANCELLED
 * @param executedAt run time
 * @param result outcome or blocking items
 * @param scheduledBy user who scheduled it
 */
public record CloseScheduleResponse(
    Long id,
    Long periodId,
    String periodName,
    Instant scheduledAt,
    String status,
    Instant executedAt,
    String result,
    String scheduledBy) {

  /**
   * Maps an entity.
   *
   * @param s schedule
   * @param periodName name of its period
   * @return response
   */
  public static CloseScheduleResponse from(PeriodCloseSchedule s, String periodName) {
    return new CloseScheduleResponse(
        s.getId(),
        s.getPeriodId(),
        periodName,
        s.getScheduledAt(),
        s.getStatus(),
        s.getExecutedAt(),
        s.getResult(),
        s.getCreatedBy());
  }
}
