package com.iortatechnxt.brokerverse.opsledger.api.dto;

import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFileInfo;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request and response records of the Disbursement queue, the extract repository and the hand-offs
 * (OPERATIONS_DESIGN section 8).
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class QueueDtos {

  private QueueDtos() {}

  /**
   * A payment request of the Disbursement queue.
   *
   * @param id id
   * @param requestNo request number
   * @param type type
   * @param sourceModule source module
   * @param sourceRef source reference
   * @param payeeCode payee
   * @param payeeName payee name
   * @param currency currency
   * @param amount amount
   * @param description description
   * @param status status
   * @param dvNo DV number
   * @param sentAt sent
   * @param acknowledgedAt acknowledged
   * @param dvAssignedAt DV assigned
   * @param paidAt paid
   * @param returnedAt returned
   * @param returnReason return reason
   * @param sentBy sender
   */
  public record DisbursementResponse(
      Long id,
      String requestNo,
      DisbursementRequest.Type type,
      String sourceModule,
      String sourceRef,
      String payeeCode,
      String payeeName,
      String currency,
      BigDecimal amount,
      String description,
      DisbursementRequest.Status status,
      String dvNo,
      Instant sentAt,
      Instant acknowledgedAt,
      Instant dvAssignedAt,
      Instant paidAt,
      Instant returnedAt,
      String returnReason,
      String sentBy) {

    /**
     * Maps a request.
     *
     * @param r request
     * @return response
     */
    public static DisbursementResponse from(DisbursementRequest r) {
      return new DisbursementResponse(
          r.getId(),
          r.getRequestNo(),
          r.getRequestType(),
          r.getSourceModule(),
          r.getSourceRef(),
          r.getPayeeCode(),
          r.getPayeeName(),
          r.getCurrency(),
          r.getAmount(),
          r.getDescription(),
          r.getStatus(),
          r.getDvNo(),
          r.getSentAt(),
          r.getAcknowledgedAt(),
          r.getDvAssignedAt(),
          r.getPaidAt(),
          r.getReturnedAt(),
          r.getReturnReason(),
          r.getCreatedBy());
    }
  }

  /**
   * DV number of a payment request.
   *
   * @param dvNo disbursement voucher number
   */
  public record DvRequest(@NotBlank @Size(max = 40) String dvNo) {}

  /**
   * A file of the extract repository.
   *
   * @param id id
   * @param folder folder
   * @param fileName file name
   * @param contentType MIME type
   * @param sizeBytes size
   * @param sha256 checksum
   * @param sourceModule module that produced it
   * @param sourceRef business reference
   * @param createdAt stored at
   * @param createdBy stored by
   */
  public record ExtractFileResponse(
      Long id,
      String folder,
      String fileName,
      String contentType,
      long sizeBytes,
      String sha256,
      String sourceModule,
      String sourceRef,
      Instant createdAt,
      String createdBy) {

    /**
     * Maps a listing entry.
     *
     * @param f file
     * @return response
     */
    public static ExtractFileResponse from(ExtractFileInfo f) {
      return new ExtractFileResponse(
          f.id(),
          f.folder(),
          f.fileName(),
          f.contentType(),
          f.sizeBytes(),
          f.sha256(),
          f.sourceModule(),
          f.sourceRef(),
          f.createdAt(),
          f.createdBy());
    }
  }

  /**
   * A hand-off of a default port adapter.
   *
   * @param id id
   * @param port port
   * @param sourceModule module asking
   * @param sourceRef its reference
   * @param reference business reference
   * @param amount amount
   * @param currency currency
   * @param summary what has to be done
   * @param status status
   * @param createdAt created
   * @param closedAt closed
   * @param closedBy closed by
   * @param closingNote what was done
   */
  public record HandoffResponse(
      Long id,
      String port,
      String sourceModule,
      String sourceRef,
      String reference,
      BigDecimal amount,
      String currency,
      String summary,
      OpsHandoff.Status status,
      Instant createdAt,
      Instant closedAt,
      String closedBy,
      String closingNote) {

    /**
     * Maps a hand-off.
     *
     * @param h hand-off
     * @return response
     */
    public static HandoffResponse from(OpsHandoff h) {
      return new HandoffResponse(
          h.getId(),
          h.getPort(),
          h.getSourceModule(),
          h.getSourceRef(),
          h.getReference(),
          h.getAmount(),
          h.getCurrency(),
          h.getSummary(),
          h.getStatus(),
          h.getCreatedAt(),
          h.getClosedAt(),
          h.getClosedBy(),
          h.getClosingNote());
    }
  }
}
