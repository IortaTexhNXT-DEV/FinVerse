package com.iortatechnxt.brokerverse.report.api.dto;

import com.iortatechnxt.brokerverse.report.domain.ReportRun;
import com.iortatechnxt.brokerverse.report.domain.ReportRunAction;
import java.time.Instant;

/**
 * An archived report run or export (CSHID.018: name, date, creator).
 *
 * @param id id
 * @param reportCode report code
 * @param title report title
 * @param category category key
 * @param parameters parameter echo
 * @param action VIEW or EXPORT
 * @param format export format
 * @param rowCount rows of the result
 * @param fileName exported file name
 * @param sizeBytes exported file size
 * @param createdBy run by
 * @param createdAt run at
 */
public record ReportRunResponse(
    Long id,
    String reportCode,
    String title,
    String category,
    String parameters,
    ReportRunAction action,
    String format,
    int rowCount,
    String fileName,
    Long sizeBytes,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps a run.
   *
   * @param r run
   * @return response
   */
  public static ReportRunResponse from(ReportRun r) {
    return new ReportRunResponse(
        r.getId(),
        r.getReportCode(),
        r.getTitle(),
        r.getCategory(),
        r.getParameters(),
        r.getAction(),
        r.getFormat(),
        r.getRowCount(),
        r.getFileName(),
        r.getSizeBytes(),
        r.getCreatedBy(),
        r.getCreatedAt());
  }
}
