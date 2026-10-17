package com.iortatechnxt.brokerverse.adjustment.api.dto;

import com.iortatechnxt.brokerverse.adjustment.domain.PostingBatch;
import java.time.Instant;
import java.util.List;

/**
 * A posting batch (ADJID.006) with the outcome of each request.
 *
 * @param batchNo validation batch number
 * @param postedCount posted
 * @param pendingCount posted, payments to re-apply
 * @param failedCount not posted
 * @param remarks remarks
 * @param createdBy user who posted
 * @param createdAt posting time
 * @param lines outcome per request (empty in lists)
 */
public record BatchResponse(
    String batchNo,
    int postedCount,
    int pendingCount,
    int failedCount,
    String remarks,
    String createdBy,
    Instant createdAt,
    List<Line> lines) {

  /** Defensive copy. */
  public BatchResponse {
    lines = List.copyOf(lines);
  }

  /**
   * Maps a batch with its lines.
   *
   * @param b batch (lines loaded)
   * @return response
   */
  public static BatchResponse from(PostingBatch b) {
    return of(
        b,
        b.getLines().stream()
            .map(
                l ->
                    new Line(
                        l.requestId(),
                        l.requestNo(),
                        l.invoiceNo(),
                        l.outcome().name(),
                        l.message()))
            .toList());
  }

  /**
   * Maps a batch without its lines (lists).
   *
   * @param b batch
   * @return response
   */
  public static BatchResponse summary(PostingBatch b) {
    return of(b, List.of());
  }

  private static BatchResponse of(PostingBatch b, List<Line> lines) {
    return new BatchResponse(
        b.getBatchNo(),
        b.getPostedCount(),
        b.getPendingCount(),
        b.getFailedCount(),
        b.getRemarks(),
        b.getCreatedBy(),
        b.getCreatedAt(),
        lines);
  }

  /**
   * Outcome of one request.
   *
   * @param requestId request
   * @param requestNo request number
   * @param invoiceNo invoice
   * @param outcome POSTED, AWAITING_REAPPLICATION or FAILED
   * @param message what happened
   */
  public record Line(
      Long requestId, String requestNo, String invoiceNo, String outcome, String message) {}
}
