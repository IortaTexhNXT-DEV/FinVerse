package com.iortatechnxt.brokerverse.bulk.api.dto;

import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJobStatus;
import java.time.Instant;

/**
 * A bulk upload.
 *
 * @param id id
 * @param jobNo upload number
 * @param handlerCode handler
 * @param fileName file
 * @param status status
 * @param totalRows rows
 * @param validRows valid rows
 * @param invalidRows invalid rows
 * @param committedRows committed rows
 * @param failedRows rows failed at commit
 * @param createdBy uploaded by
 * @param createdAt uploaded at
 * @param completedAt completed or cancelled at
 * @param fileSha256 SHA-256 of the uploaded file
 * @param reprocessCount times the failed rows were reprocessed
 */
public record BulkJobResponse(
    Long id,
    String jobNo,
    String handlerCode,
    String fileName,
    BulkJobStatus status,
    int totalRows,
    int validRows,
    int invalidRows,
    int committedRows,
    int failedRows,
    String createdBy,
    Instant createdAt,
    Instant completedAt,
    String fileSha256,
    int reprocessCount) {

  /**
   * Maps a job.
   *
   * @param j job
   * @return response
   */
  public static BulkJobResponse from(BulkJob j) {
    return new BulkJobResponse(
        j.getId(),
        j.getJobNo(),
        j.getHandlerCode(),
        j.getFileName(),
        j.getStatus(),
        j.getTotalRows(),
        j.getValidRows(),
        j.getInvalidRows(),
        j.getCommittedRows(),
        j.getFailedRows(),
        j.getCreatedBy(),
        j.getCreatedAt(),
        j.getCompletedAt(),
        j.getFileSha256(),
        j.getReprocessCount());
  }
}
