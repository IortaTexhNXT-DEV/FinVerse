package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequest.Origin;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RequestSource;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.SpecialStage;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A special remittance request {@code SPR-<yyyy>} (MKTID.009, RMTID.030): an invoice to remit
 * outside the regular schedule for a condition (LOV {@code SPECIAL_REMIT_CONDITION}: claims,
 * renewal, installment due, immediate OR). Validated, approved, then remitted through its own
 * SPECIAL batch in Process Remittance; notified at every change (RMTID.033).
 */
@Entity
@Table(name = "rem_special_request")
public class SpecialRemittance extends BaseEntity {

  private static final int MAX_TEXT = 500;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "request_no", nullable = false, length = 30, updatable = false)
  private String requestNo;

  @Embedded private InvoiceRef invoice;

  @Column(name = "policy_no", length = 60, updatable = false)
  private String policyNo;

  @Column(length = 40, updatable = false)
  private String segment;

  @Column(name = "condition_code", nullable = false, length = 40, updatable = false)
  private String conditionCode;

  @Column(length = MAX_TEXT, updatable = false)
  private String remarks;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private SpecialStage stage = SpecialStage.REQUESTED;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private RequestSource source;

  @Column(name = "requested_by", nullable = false, length = 50, updatable = false)
  private String requestedBy;

  @Column(name = "validation_note", length = MAX_TEXT)
  private String validationNote;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "rejected_reason", length = MAX_TEXT)
  private String rejectedReason;

  @Column(name = "batch_no", length = 40)
  private String batchNo;

  @Column(name = "pushed_at")
  private Instant pushedAt;

  protected SpecialRemittance() {}

  /**
   * A new request.
   *
   * @param companyId company
   * @param requestNo request number
   * @param invoice invoice facts
   * @param details policy, segment, condition and remarks
   * @param origin source and requestor
   */
  public SpecialRemittance(
      Long companyId, String requestNo, InvoiceRef invoice, Details details, Origin origin) {
    this.companyId = companyId;
    this.requestNo = requestNo;
    this.invoice = invoice;
    this.policyNo = details.policyNo();
    this.segment = details.segment();
    this.conditionCode = details.conditionCode();
    this.remarks = details.remarks();
    this.source = origin.source();
    this.requestedBy = origin.requestedBy();
  }

  /**
   * Keeps the outcome of the system checks.
   *
   * @param note note
   */
  public void validated(String note) {
    this.validationNote = note;
  }

  /**
   * Records the approval, refused to the requestor (four eyes).
   *
   * @param by approver
   * @param at time
   */
  public void approved(String by, Instant at) {
    if (CurrentUser.sameUser(by, requestedBy)) {
      throw new BusinessRuleException(
          "SPECIAL_REMIT_FOUR_EYES", "Request " + requestNo + " must be approved by another user");
    }
    this.approvedBy = by;
    this.approvedAt = at;
  }

  /**
   * Records the rejection.
   *
   * @param reason reason
   */
  public void rejected(String reason) {
    this.rejectedReason = reason;
  }

  /**
   * Links the special batch.
   *
   * @param number batch number
   */
  public void inBatch(String number) {
    this.batchNo = number;
  }

  /**
   * Records the push to Disbursement.
   *
   * @param at time
   */
  public void pushed(Instant at) {
    this.pushedAt = at;
  }

  /**
   * Mirrors the workflow stage.
   *
   * @param newStage stage
   */
  public void markStage(SpecialStage newStage) {
    this.stage = newStage;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public String getInvoiceNo() {
    return invoice.invoiceNo();
  }

  public String getArn() {
    return invoice.arn();
  }

  public String getInsurerCode() {
    return invoice.insurerCode();
  }

  public String getClientCode() {
    return invoice.clientCode();
  }

  public String getAssuredName() {
    return invoice.assuredName();
  }

  public String getPolicyNo() {
    return policyNo;
  }

  public String getSegment() {
    return segment;
  }

  public String getConditionCode() {
    return conditionCode;
  }

  public String getRemarks() {
    return remarks;
  }

  public SpecialStage getStage() {
    return stage;
  }

  public RequestSource getSource() {
    return source;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public String getValidationNote() {
    return validationNote;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public String getRejectedReason() {
    return rejectedReason;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public Instant getPushedAt() {
    return pushedAt;
  }

  /**
   * What is requested.
   *
   * @param policyNo policy
   * @param segment marketing segment
   * @param conditionCode condition (LOV SPECIAL_REMIT_CONDITION)
   * @param remarks remarks
   */
  public record Details(String policyNo, String segment, String conditionCode, String remarks) {}
}
