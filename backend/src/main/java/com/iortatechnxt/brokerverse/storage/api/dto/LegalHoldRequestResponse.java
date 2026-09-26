package com.iortatechnxt.brokerverse.storage.api.dto;

import com.iortatechnxt.brokerverse.storage.domain.LegalHoldRequest;
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
    String decisionNote) {

  /**
   * Maps a request.
   *
   * @param r request
   * @return response
   */
  public static LegalHoldRequestResponse from(LegalHoldRequest r) {
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
        r.getDecisionNote());
  }
}
