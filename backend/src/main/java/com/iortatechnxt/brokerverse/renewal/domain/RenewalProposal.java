package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A proposal of a renewal account (FRRN.017, FRRN.018): a Quick or Full Proposal generated from the
 * account, or the proposal completed by TSU, with its signatories and its sending through CCM.
 */
@Entity
@Table(name = "rnw_proposal")
public class RenewalProposal extends BaseEntity {

  /** Kind: Quick Proposal. */
  public static final String QUICK = "QUICK";

  /** Kind: Full Proposal. */
  public static final String FULL = "FULL";

  /** Kind: proposal completed by TSU. */
  public static final String TSU = "TSU";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "proposal_no", nullable = false, length = 30, updatable = false)
  private String proposalNo;

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Column(nullable = false, length = 10, updatable = false)
  private String kind;

  @Column(name = "file_name", nullable = false, length = 200, updatable = false)
  private String fileName;

  @Column(name = "attachment_id")
  private Long attachmentId;

  @Column(length = 500, updatable = false)
  private String signatories;

  @Column(nullable = false, length = 20)
  private String status = "GENERATED";

  @Column(name = "message_no", length = 30)
  private String messageNo;

  @Column(length = 1000)
  private String recipients;

  /** For JPA. */
  protected RenewalProposal() {}

  /**
   * A generated proposal.
   *
   * @param c renewal
   * @param proposalNo number
   * @param kind QUICK, FULL or TSU
   * @param fileName file name
   * @param signatories signatories, comma-separated
   */
  public RenewalProposal(
      RenewalCandidate c, String proposalNo, String kind, String fileName, String signatories) {
    this.companyId = c.getCompanyId();
    this.candidateId = c.getId();
    this.proposalNo = proposalNo;
    this.kind = kind;
    this.fileName = fileName;
    this.signatories = signatories;
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
   * Records the sending.
   *
   * @param message CCM message
   * @param to recipients
   */
  public void sent(String message, String to) {
    this.status = "SENT";
    this.messageNo = message;
    this.recipients = to;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getProposalNo() {
    return proposalNo;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getKind() {
    return kind;
  }

  public String getFileName() {
    return fileName;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public String getSignatories() {
    return signatories;
  }

  public String getStatus() {
    return status;
  }

  public String getMessageNo() {
    return messageNo;
  }

  public String getRecipients() {
    return recipients;
  }
}
