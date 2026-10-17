package com.iortatechnxt.brokerverse.storage.api.dto;

import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A stored file as the records officers see it on the Legal Holds screen (no content, no link).
 *
 * @param id file number
 * @param fileName file name
 * @param documentType document type
 * @param recordClass record class
 * @param legalHold under legal hold
 * @param legalHoldReason reason of the hold
 * @param retentionUntil end of retention
 * @param createdBy uploader
 * @param createdAt upload time
 */
public record HoldFileResponse(
    Long id,
    String fileName,
    String documentType,
    String recordClass,
    boolean legalHold,
    String legalHoldReason,
    LocalDate retentionUntil,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps a file.
   *
   * @param f file
   * @return response
   */
  public static HoldFileResponse from(StoredFile f) {
    return new HoldFileResponse(
        f.getId(),
        f.getFileName(),
        f.getDocumentType(),
        f.getRecordClass(),
        f.isLegalHold(),
        f.getLegalHoldReason(),
        f.getRetentionUntil(),
        f.getCreatedBy(),
        f.getCreatedAt());
  }
}
