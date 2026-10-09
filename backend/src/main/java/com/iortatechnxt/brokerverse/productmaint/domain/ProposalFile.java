package com.iortatechnxt.brokerverse.productmaint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A proposal slip of one insurer and version (BDOI FRS FRPM.009.02 and FRPM.013.01): the file name,
 * the stored document, who generated it and when.
 */
@Entity
@Table(name = "pm_proposal_file")
public class ProposalFile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "record_type", nullable = false, length = 30, updatable = false)
  private String recordType;

  @Column(name = "record_id", nullable = false, updatable = false)
  private Long recordId;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "insurer_name", nullable = false, length = 200, updatable = false)
  private String insurerName;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Column(name = "file_name", nullable = false, length = 200, updatable = false)
  private String fileName;

  @Column(name = "attachment_id", updatable = false)
  private Long attachmentId;

  @Column(name = "generated_by", nullable = false, length = 50, updatable = false)
  private String generatedBy;

  @Column(name = "generated_at", nullable = false, updatable = false)
  private Instant generatedAt;

  /** For JPA. */
  protected ProposalFile() {}

  /**
   * Records a generated proposal slip.
   *
   * @param record the record
   * @param insurer insurer code and name
   * @param versionNo version of the insurer's slip
   * @param fileName file name
   * @param attachmentId stored document
   * @param generated who and when
   */
  public ProposalFile(
      TermsRecord record,
      String[] insurer,
      int versionNo,
      String fileName,
      Long attachmentId,
      Generated generated) {
    this.recordType = record.type();
    this.recordId = record.id();
    this.insurerCode = insurer[0];
    this.insurerName = insurer[1];
    this.versionNo = versionNo;
    this.fileName = fileName;
    this.attachmentId = attachmentId;
    this.generatedBy = generated.by();
    this.generatedAt = generated.at();
  }

  public Long getId() {
    return id;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getInsurerName() {
    return insurerName;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public String getFileName() {
    return fileName;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public String getGeneratedBy() {
    return generatedBy;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  /**
   * Who generated a file and when.
   *
   * @param by user
   * @param at time
   */
  public record Generated(String by, Instant at) {}
}
