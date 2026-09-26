package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A document in the EB register (BRID-007, 008, 024, 025; design 4.2): the attachment with its
 * document type, process tag and version on a programme and optionally a cycle, and where it came
 * from. A new version supersedes the previous one; rows are never changed otherwise.
 */
@Entity
@Table(name = "eb_document")
public class EbDocument extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "programme_id", nullable = false, updatable = false)
  private Long programmeId;

  @Column(name = "cycle_id", updatable = false)
  private Long cycleId;

  @Column(name = "document_type", nullable = false, length = 40, updatable = false)
  private String documentType;

  @Column(name = "process_type", nullable = false, length = 40, updatable = false)
  private String processType;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Column(name = "attachment_id", nullable = false, updatable = false)
  private Long attachmentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private EbDocumentSource source;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EbDocumentStatus status = EbDocumentStatus.ACTIVE;

  protected EbDocument() {}

  /**
   * Registers a document version.
   *
   * @param companyId company
   * @param place programme and cycle (null for a programme document)
   * @param documentType document type (list DOCUMENT_TYPE)
   * @param processType process tag (list EB_PROCESS_TYPE)
   * @param versionNo version number, from 1
   * @param attachmentId the stored file
   * @param source where it came from
   */
  public EbDocument(
      Long companyId,
      Place place,
      String documentType,
      String processType,
      int versionNo,
      Long attachmentId,
      EbDocumentSource source) {
    this.companyId = companyId;
    this.programmeId = place.programmeId();
    this.cycleId = place.cycleId();
    this.documentType = documentType;
    this.processType = processType;
    this.versionNo = versionNo;
    this.attachmentId = attachmentId;
    this.source = source;
  }

  /** A later version replaces this one. */
  public void supersede() {
    this.status = EbDocumentStatus.SUPERSEDED;
  }

  /** The document was rejected on validation. */
  public void reject() {
    this.status = EbDocumentStatus.REJECTED;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getProgrammeId() {
    return programmeId;
  }

  public Long getCycleId() {
    return cycleId;
  }

  public String getDocumentType() {
    return documentType;
  }

  public String getProcessType() {
    return processType;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public EbDocumentSource getSource() {
    return source;
  }

  public EbDocumentStatus getStatus() {
    return status;
  }

  /**
   * Where a document belongs.
   *
   * @param programmeId programme
   * @param cycleId cycle, null for a programme document
   */
  public record Place(Long programmeId, Long cycleId) {}
}
