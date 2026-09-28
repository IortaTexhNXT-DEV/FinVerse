package com.iortatechnxt.brokerverse.storage.api.dto;

import com.iortatechnxt.brokerverse.storage.domain.LegalHoldRequest;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import java.time.Instant;

/**
 * A legal hold request.
 *
 * @param id id
 * @param storedFileId file
 * @param action PLACE or RELEASE
 * @param reason reason
 * @param status PENDING, APPROVED or REJECTED
 * @param requestedBy requester
 * @param requestedAt request time
 * @param decidedBy approver
 * @param decidedAt decision time
 * @param decisionNote decision note
 * @param fileName name of the file (null when not looked up)
 * @param documentType document type of the file (null when not looked up)
 */
public record LegalHoldRequestResponse(
    Long id,
    Long storedFileId,
    String action,
    String reason,
    String status,
    String requestedBy,
    Instant requestedAt,
    String decidedBy,
    Instant decidedAt,
    String decisionNote,
    String fileName,
    String documentType) {

  /**
   * Maps a request.
   *
   * @param r request
   * @return response
   */
  public static LegalHoldRequestResponse from(LegalHoldRequest r) {
    return from(r, null);
  }

  /**
   * Maps a request with the file it concerns.
   *
   * @param r request
   * @param file the file, or null
   * @return response
   */
  public static LegalHoldRequestResponse from(LegalHoldRequest r, StoredFile file) {
    return new LegalHoldRequestResponse(
        r.getId(),
        r.getStoredFileId(),
        r.getAction().name(),
        r.getReason(),
        r.getStatus().name(),
        r.getRequestedBy(),
        r.getRequestedAt(),
        r.getDecidedBy(),
        r.getDecidedAt(),
        r.getDecisionNote(),
        file == null ? null : file.getFileName(),
        file == null ? null : file.getDocumentType());
  }
}
