package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A quotation request of a renewal account For Quotation to TSU (FRRN.018.01): submitted for the
 * Team Lead's approval, approved, rejected or returned for revision, assigned to a TSU Officer and
 * completed with the comparative table and the proposal.
 */
@Entity
@Table(name = "rnw_tsu_request")
public class TsuRequest extends BaseEntity {

  /** Status: being prepared. */
  public static final String DRAFT = "DRAFT";

  /** Status: waiting for the Team Lead. */
  public static final String PENDING = "PENDING_TL_APPROVAL";

  /** Status: returned by the Team Lead for revision. */
  public static final String RETURNED = "RETURNED_FOR_REVISION";

  /** Status: rejected by the Team Lead. */
  public static final String REJECTED = "REJECTED";

  /** Status: approved, waiting for the TSU Officer. */
  public static final String APPROVED = "APPROVED";

  /** Status: with the TSU Officer. */
  public static final String PROCESSING = "FOR_TSU_PROCESSING";

  /** Status: proposal completed by TSU. */
  public static final String COMPLETED = "COMPLETED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "request_no", nullable = false, length = 30, updatable = false)
  private String requestNo;

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Column(nullable = false, length = 30)
  private String status = DRAFT;

  @Column(length = 1000)
  private String remarks;

  @Column(name = "decision_remarks", length = 1000)
  private String decisionRemarks;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "tsu_officer", length = 50)
  private String tsuOfficer;

  @Column(name = "proposal_attachment_id")
  private Long proposalAttachmentId;

  /** For JPA. */
  protected TsuRequest() {}

  /**
   * A new request.
   *
   * @param c renewal
   * @param requestNo number
   * @param remarks remarks
   */
  public TsuRequest(RenewalCandidate c, String requestNo, String remarks) {
    this.companyId = c.getCompanyId();
    this.candidateId = c.getId();
    this.requestNo = requestNo;
    this.remarks = remarks;
  }

  /**
   * Moves the request to a status.
   *
   * @param next status
   */
  public void status(String next) {
    this.status = next;
  }

  /**
   * Records the Team Lead's decision.
   *
   * @param next status
   * @param user Team Lead
   * @param text remarks
   */
  public void decide(String next, String user, String text) {
    this.status = next;
    this.decidedBy = user;
    this.decisionRemarks = text;
  }

  /**
   * Assigns the TSU Officer.
   *
   * @param officer user
   */
  public void assign(String officer) {
    this.tsuOfficer = officer;
    this.status = PROCESSING;
  }

  /**
   * Completes the request with the proposal.
   *
   * @param attachmentId stored proposal
   */
  public void complete(Long attachmentId) {
    this.proposalAttachmentId = attachmentId;
    this.status = COMPLETED;
  }

  /**
   * Whether the request is still open.
   *
   * @return false once rejected or cancelled
   */
  public boolean isOpen() {
    return !REJECTED.equals(status) && !"CANCELLED".equals(status);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getStatus() {
    return status;
  }

  public String getRemarks() {
    return remarks;
  }

  public String getDecisionRemarks() {
    return decisionRemarks;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public String getTsuOfficer() {
    return tsuOfficer;
  }

  public Long getProposalAttachmentId() {
    return proposalAttachmentId;
  }
}
