package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessBatchStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestBatch;
import java.time.Instant;

/**
 * A bulk access request batch (BRD 1.009).
 *
 * @param id id
 * @param batchNo batch number (the upload number)
 * @param lines number of line requests
 * @param status status mirrored from the lines
 * @param remarks batch remarks
 * @param createdBy requester
 * @param createdAt upload time
 */
public record AccessBatchResponse(
    Long id,
    String batchNo,
    int lines,
    AccessBatchStatus status,
    String remarks,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps a batch.
   *
   * @param b batch
   * @return response
   */
  public static AccessBatchResponse from(AccessRequestBatch b) {
    return new AccessBatchResponse(
        b.getId(),
        b.getBatchNo(),
        b.getLines(),
        b.getStatus(),
        b.getRemarks(),
        b.getCreatedBy(),
        b.getCreatedAt());
  }
}
