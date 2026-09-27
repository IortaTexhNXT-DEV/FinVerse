package com.iortatechnxt.brokerverse.screening.cases.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * The metadata of a document uploaded on a case (SNSRP-601; FR-SS-052): form type, document type,
 * date received, source, the sequence among the case's documents of the type and the name of the
 * BRD naming convention {@code <Form Type>_<Client Name>_<Date Received>_<Document Type>_<n>}. The
 * file itself is an attachment of entity type {@code ScreeningCase}.
 */
@Entity
@Table(name = "scr_case_document")
public class CaseDocument extends BaseEntity {

  @Column(name = "case_id", nullable = false, updatable = false)
  private Long caseId;

  @Column(name = "attachment_id", nullable = false, updatable = false)
  private Long attachmentId;

  @Column(name = "form_type", nullable = false, length = 30, updatable = false)
  private String formType;

  @Column(name = "document_type", nullable = false, length = 40, updatable = false)
  private String documentType;

  @Column(name = "date_received", nullable = false, updatable = false)
  private LocalDate dateReceived;

  @Column(name = "source", nullable = false, length = 100, updatable = false)
  private String source;

  @Column(name = "sequence_no", nullable = false, updatable = false)
  private int sequenceNo;

  @Column(name = "nominated_name", nullable = false, length = 300, updatable = false)
  private String nominatedName;

  @Column(name = "kyc_registered", nullable = false)
  private boolean kycRegistered;

  /** For JPA. */
  protected CaseDocument() {}

  /**
   * Records a document.
   *
   * @param caseId the case
   * @param attachmentId the stored file
   * @param meta form type, document type, date received and source
   * @param sequenceNo the sequence among the case's documents of the type
   * @param nominatedName the name of the naming convention
   */
  public CaseDocument(
      Long caseId, Long attachmentId, DocumentMeta meta, int sequenceNo, String nominatedName) {
    this.caseId = caseId;
    this.attachmentId = attachmentId;
    this.formType = meta.formType();
    this.documentType = meta.documentType();
    this.dateReceived = meta.dateReceived();
    this.source = meta.source();
    this.sequenceNo = sequenceNo;
    this.nominatedName = nominatedName;
  }

  /** Marks the document as registered on the client's KYC documents. */
  public void registeredOnClient() {
    this.kycRegistered = true;
  }

  public Long getCaseId() {
    return caseId;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public String getFormType() {
    return formType;
  }

  public String getDocumentType() {
    return documentType;
  }

  public LocalDate getDateReceived() {
    return dateReceived;
  }

  public String getSource() {
    return source;
  }

  public int getSequenceNo() {
    return sequenceNo;
  }

  public String getNominatedName() {
    return nominatedName;
  }

  public boolean isKycRegistered() {
    return kycRegistered;
  }

  /**
   * The metadata entered with a document (FR-SS-052 upload dialog).
   *
   * @param formType LOV SCR_FORM_TYPE
   * @param documentType LOV SCR_DOCUMENT_TYPE
   * @param dateReceived date received, not in the future
   * @param source where it came from (client, branch, Marketing...)
   */
  public record DocumentMeta(
      String formType, String documentType, LocalDate dateReceived, String source) {}
}
