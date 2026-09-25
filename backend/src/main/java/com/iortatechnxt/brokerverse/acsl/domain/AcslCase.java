package com.iortatechnxt.brokerverse.acsl.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * An ACSL case (ACSL 2.5.0-2.6.2; ACCOUNTING_DISBURSEMENT_DESIGN 5.3): an account investigation, an
 * account analysis request (the validation of a refund of a cancelled account asked by payrequest,
 * MKT 1.11.0), an AR refund application or a sub-ledger payment reversal. It records the findings,
 * the result given to the requester, the correction entry it raised and the reversal it requested
 * from Cashiering. Its stage mirrors the ACSL_CASE work case.
 */
@Entity
@Table(name = "acsl_case")
public class AcslCase extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "case_no", nullable = false, length = 30, updatable = false)
  private String caseNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "case_type", nullable = false, length = 30, updatable = false)
  private CaseType caseType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private CaseStage stage = CaseStage.RECEIVED;

  @Embedded private CaseSubject account;

  @Column(nullable = false, length = 250)
  private String subject;

  @Column(length = 2000)
  private String details;

  @Column(name = "requester_module", length = 30, updatable = false)
  private String requesterModule;

  @Column(name = "requester_ref", length = 80, updatable = false)
  private String requesterRef;

  @Column(name = "requested_by", nullable = false, length = 50, updatable = false)
  private String requestedBy;

  @Column(length = 4000)
  private String findings;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private CaseOutcome outcome;

  @Column(name = "result_remarks", length = 1000)
  private String resultRemarks;

  @Column(name = "result_by", length = 50)
  private String resultBy;

  @Column(name = "result_at")
  private Instant resultAt;

  @Column(name = "correction_id")
  private Long correctionId;

  @Column(name = "reversal_ref", length = 40)
  private String reversalRef;

  @Column(name = "reversal_status", length = 20)
  private String reversalStatus;

  @Column(name = "reversal_message", length = 500)
  private String reversalMessage;

  protected AcslCase() {}

  /**
   * Opens a case.
   *
   * @param companyId company
   * @param caseNo case number
   * @param caseType type
   * @param account account concerned
   * @param subject one-line subject
   * @param requester requesting module, reference and user
   */
  public AcslCase(
      Long companyId,
      String caseNo,
      CaseType caseType,
      CaseSubject account,
      String subject,
      Requester requester) {
    this.companyId = companyId;
    this.caseNo = caseNo;
    this.caseType = caseType;
    this.account = account;
    this.subject = subject;
    this.requesterModule = requester.module();
    this.requesterRef = requester.reference();
    this.requestedBy = requester.user();
    this.details = requester.details();
  }

  /**
   * Mirrors the work case stage.
   *
   * @param newStage stage
   */
  public void moveTo(CaseStage newStage) {
    this.stage = newStage;
  }

  /**
   * Records the findings of the investigation (ACSL 2.5.0).
   *
   * @param text findings
   */
  public void recordFindings(String text) {
    this.findings = text;
  }

  /**
   * Records the result given to the requester (ACSL 2.5.4).
   *
   * @param newOutcome outcome
   * @param remarks remarks
   * @param user processor
   * @param at when
   */
  public void recordResult(CaseOutcome newOutcome, String remarks, String user, Instant at) {
    this.outcome = newOutcome;
    this.resultRemarks = remarks;
    this.resultBy = user;
    this.resultAt = at;
  }

  /**
   * Links the correction entry raised from the case (ACSL 2.9.0).
   *
   * @param id correction
   */
  public void linkCorrection(Long id) {
    this.correctionId = id;
  }

  /**
   * Records where the payment reversal requested from Cashiering stands (ACSL 2.6.1).
   *
   * @param ref Cashiering reference or hand-off
   * @param status SUBMITTED, DEFERRED, APPROVED or REJECTED
   * @param message message
   */
  public void reversal(String ref, String status, String message) {
    this.reversalRef = ref;
    this.reversalStatus = status;
    this.reversalMessage = message;
  }

  /**
   * The account, never null.
   *
   * @return account
   */
  public CaseSubject accountOrNone() {
    return account == null ? CaseSubject.NONE : account;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getCaseNo() {
    return caseNo;
  }

  public CaseType getCaseType() {
    return caseType;
  }

  public CaseStage getStage() {
    return stage;
  }

  public String getSubject() {
    return subject;
  }

  public String getDetails() {
    return details;
  }

  public String getRequesterModule() {
    return requesterModule;
  }

  public String getRequesterRef() {
    return requesterRef;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public String getFindings() {
    return findings;
  }

  public CaseOutcome getOutcome() {
    return outcome;
  }

  public String getResultRemarks() {
    return resultRemarks;
  }

  public String getResultBy() {
    return resultBy;
  }

  public Instant getResultAt() {
    return resultAt;
  }

  public Long getCorrectionId() {
    return correctionId;
  }

  public String getReversalRef() {
    return reversalRef;
  }

  public String getReversalStatus() {
    return reversalStatus;
  }

  public String getReversalMessage() {
    return reversalMessage;
  }

  /**
   * Who asked for a case.
   *
   * @param module requesting module (PAYREQUEST), null for a case opened in ACSL
   * @param reference its reference (idempotency key), may be null
   * @param user requesting user
   * @param details what is asked, may be null
   */
  public record Requester(String module, String reference, String user, String details) {}
}
