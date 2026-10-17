package com.iortatechnxt.brokerverse.report.api.dto;

import com.iortatechnxt.brokerverse.report.domain.ReportBatch;
import com.iortatechnxt.brokerverse.report.domain.ReportBatchItem;
import java.time.Instant;
import java.util.List;

/**
 * Report batch view.
 *
 * @param id id
 * @param batchNo batch number
 * @param format file format
 * @param mergedPdf merged PDF
 * @param paper paper
 * @param orientation orientation
 * @param status COMPLETED, PARTIAL or FAILED
 * @param fileName file name, null when nothing was produced
 * @param sizeBytes file size
 * @param createdAt started at
 * @param completedAt completed at
 * @param items reports and outcomes (empty in lists)
 */
public record ReportBatchResponse(
    Long id,
    String batchNo,
    String format,
    boolean mergedPdf,
    String paper,
    String orientation,
    String status,
    String fileName,
    Long sizeBytes,
    Instant createdAt,
    Instant completedAt,
    List<Item> items) {

  /**
   * Maps a batch with its items.
   *
   * @param b batch
   * @return response
   */
  public static ReportBatchResponse from(ReportBatch b) {
    return map(b, b.getItems().stream().map(Item::from).toList());
  }

  /**
   * Maps a batch without items (lists).
   *
   * @param b batch
   * @return response
   */
  public static ReportBatchResponse summary(ReportBatch b) {
    return map(b, List.of());
  }

  private static ReportBatchResponse map(ReportBatch b, List<Item> items) {
    return new ReportBatchResponse(
        b.getId(),
        b.getBatchNo(),
        b.getFormat(),
        b.isMergedPdf(),
        b.getPaper(),
        b.getOrientation(),
        b.getStatus(),
        b.getFileName(),
        b.getSizeBytes(),
        b.getCreatedAt(),
        b.getCompletedAt(),
        items);
  }

  /**
   * One report of the batch.
   *
   * @param itemNo position
   * @param reportCode report
   * @param status OK or FAILED
   * @param rowCount rows
   * @param error reason of a failure
   */
  public record Item(int itemNo, String reportCode, String status, int rowCount, String error) {

    /**
     * Maps an item.
     *
     * @param i item
     * @return response
     */
    public static Item from(ReportBatchItem i) {
      return new Item(
          i.getItemNo(), i.getReportCode(), i.getStatus(), i.getRowCount(), i.getError());
    }
  }
}
