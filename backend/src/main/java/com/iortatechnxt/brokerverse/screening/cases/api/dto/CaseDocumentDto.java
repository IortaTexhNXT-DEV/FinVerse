package com.iortatechnxt.brokerverse.screening.cases.api.dto;

import com.iortatechnxt.brokerverse.screening.cases.domain.CaseDocument;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A document of a case with its metadata and nominated name (FR-SS-052 Documents tab).
 *
 * @param id metadata id
 * @param attachmentId the file
 * @param formType form type
 * @param documentType document type
 * @param dateReceived date received
 * @param source source
 * @param sequenceNo sequence of the type
 * @param nominatedName name of the naming convention
 * @param kycRegistered registered on the client's KYC documents
 * @param createdAt uploaded at
 * @param createdBy uploaded by
 */
public record CaseDocumentDto(
    Long id,
    Long attachmentId,
    String formType,
    String documentType,
    LocalDate dateReceived,
    String source,
    int sequenceNo,
    String nominatedName,
    boolean kycRegistered,
    Instant createdAt,
    String createdBy) {

  /**
   * Maps a document.
   *
   * @param d the document
   * @return the DTO
   */
  public static CaseDocumentDto from(CaseDocument d) {
    return new CaseDocumentDto(
        d.getId(),
        d.getAttachmentId(),
        d.getFormType(),
        d.getDocumentType(),
        d.getDateReceived(),
        d.getSource(),
        d.getSequenceNo(),
        d.getNominatedName(),
        d.isKycRegistered(),
        d.getCreatedAt(),
        d.getCreatedBy());
  }
}
