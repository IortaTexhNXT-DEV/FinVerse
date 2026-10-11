package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A version of the Insurance Advice of a mortgaged renewal account (FRRN.032): generated when the
 * placement is sent and again when the placement changes; earlier versions are kept.
 */
@Entity
@Table(name = "rnw_insurance_advice")
public class AdviceVersion extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Column(name = "file_name", nullable = false, length = 200, updatable = false)
  private String fileName;

  @Column(name = "attachment_id")
  private Long attachmentId;

  @Column(nullable = false, length = 64, updatable = false)
  private String fingerprint;

  @Column(name = "trigger_event", nullable = false, length = 30, updatable = false)
  private String triggerEvent;

  @Column(name = "message_no", length = 30)
  private String messageNo;

  /** For JPA. */
  protected AdviceVersion() {}

  /**
   * A generated version.
   *
   * @param c renewal
   * @param versionNo version number
   * @param fileName file name
   * @param fingerprint digest of the placement information
   * @param triggerEvent what generated it
   */
  public AdviceVersion(
      RenewalCandidate c, int versionNo, String fileName, String fingerprint, String triggerEvent) {
    this.companyId = c.getCompanyId();
    this.candidateId = c.getId();
    this.versionNo = versionNo;
    this.fileName = fileName;
    this.fingerprint = fingerprint;
    this.triggerEvent = triggerEvent;
  }

  /**
   * Records the stored file.
   *
   * @param id attachment
   */
  public void stored(Long id) {
    this.attachmentId = id;
  }

  /**
   * Records the CCM message of the sending.
   *
   * @param message message number
   */
  public void sent(String message) {
    this.messageNo = message;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getCandidateId() {
    return candidateId;
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

  public String getFingerprint() {
    return fingerprint;
  }

  public String getTriggerEvent() {
    return triggerEvent;
  }

  public String getMessageNo() {
    return messageNo;
  }
}
