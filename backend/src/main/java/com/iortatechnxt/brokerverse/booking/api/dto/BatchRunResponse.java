package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.BatchRow;
import com.iortatechnxt.brokerverse.booking.domain.BatchRun;
import com.iortatechnxt.brokerverse.booking.domain.BatchTrigger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * A booking batch run with its per-account results.
 *
 * @param id id
 * @param runNo run number
 * @param trigger what started it
 * @param businessDate business date
 * @param startedBy user
 * @param startedAt start
 * @param finishedAt end
 * @param bookedCount accounts booked
 * @param failedCount accounts failed
 * @param rows per-account results (empty in lists)
 */
public record BatchRunResponse(
    Long id,
    String runNo,
    BatchTrigger trigger,
    LocalDate businessDate,
    String startedBy,
    Instant startedAt,
    Instant finishedAt,
    int bookedCount,
    int failedCount,
    List<BatchRow> rows) {

  /**
   * Maps a run with its rows (loaded).
   *
   * @param r run
   * @return response
   */
  public static BatchRunResponse from(BatchRun r) {
    return of(r, r.getRows());
  }

  /**
   * Maps a run for a list (rows not loaded).
   *
   * @param r run
   * @return response
   */
  public static BatchRunResponse summary(BatchRun r) {
    return of(r, List.of());
  }

  private static BatchRunResponse of(BatchRun r, List<BatchRow> rows) {
    return new BatchRunResponse(
        r.getId(),
        r.getRunNo(),
        r.getTrigger(),
        r.getBusinessDate(),
        r.getStartedBy(),
        r.getStartedAt(),
        r.getFinishedAt(),
        r.getBookedCount(),
        r.getFailedCount(),
        rows);
  }
}
