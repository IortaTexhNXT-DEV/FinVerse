package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.HoldStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RequestSource;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A Marketing request to hold an invoice from remittance until a date {@code HLD-<yyyy>}
 * (MKTID.003/005/007): reason (LOV {@code HOLD_REASON}), hold-until date and extensions, approval
 * (MKTID.006), the processor it is assigned to (MKTID.004), release or expiry (MKTID.002,
 * RMTID.021). While active, the invoice carries the {@code HOLD} flag and is not extracted
 * (RMTID.020/031).
 */
@Entity
@Table(name = "rem_hold_request")
public class HoldRequest extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "request_no", nullable = false, length = 30, updatable = false)
  private String requestNo;

  @Embedded private InvoiceRef invoice;

  @Column(name = "reason_code", nullable = false, length = 40)
  private String reasonCode;

  @Column(length = 500)
  private String remarks;

  @Column(name = "hold_until", nullable = false)
  private LocalDate holdUntil;

  @Column(name = "requested_until")
  private LocalDate requestedUntil;

  @Column(name = "extension_count", nullable = false)
  private int extensionCount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private HoldStage stage = HoldStage.DRAFT;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private RequestSource source;

  @Column(name = "requested_by", nullable = false, length = 50, updatable = false)
  private String requestedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "assigned_processor", length = 50)
  private String assignedProcessor;

  @Enumerated(EnumType.STRING)
  @Column(name = "prev_remittance_status", length = 30)
  private RemittanceStatus previousStatus;

  @Column(name = "expiry_notified", nullable = false)
  private boolean expiryNotified;

  @Column(name = "released_at")
  private Instant releasedAt;

  @Column(name = "release_note", length = 500)
  private String releaseNote;

  protected HoldRequest() {}

  /**
   * A new draft.
   *
   * @param companyId company
   * @param requestNo request number
   * @param invoice invoice facts
   * @param terms reason, remarks and date
   * @param origin source and requestor
   */
  public HoldRequest(
      Long companyId, String requestNo, InvoiceRef invoice, Terms terms, Origin origin) {
    this.companyId = companyId;
    this.requestNo = requestNo;
    this.invoice = invoice;
    this.source = origin.source();
    this.requestedBy = origin.requestedBy();
    apply(terms);
  }

  /**
   * Changes the terms of a draft.
   *
   * @param terms reason, remarks and date
   */
  public void update(Terms terms) {
    apply(terms);
  }

  private void apply(Terms terms) {
    this.reasonCode = terms.reasonCode();
    this.remarks = terms.remarks();
    this.holdUntil = terms.holdUntil();
  }

  /**
   * Records the submission and the status the invoice had.
   *
   * @param status previous remittance status
   * @param at time
   */
  public void submitted(RemittanceStatus status, Instant at) {
    this.previousStatus = status;
    this.submittedAt = at;
  }

  /**
   * Records an approval (MKTID.006), refused to the requestor (four eyes).
   *
   * @param by approver
   * @param at time
   */
  public void approved(String by, Instant at) {
    if (CurrentUser.sameUser(by, requestedBy)) {
      throw new BusinessRuleException(
          "HOLD_FOUR_EYES", "Hold " + requestNo + " must be approved by another user");
    }
    this.approvedBy = by;
    this.approvedAt = at;
  }

  /**
   * Asks for a later hold-until date (MKTID.005).
   *
   * @param until new date
   */
  public void requestExtension(LocalDate until) {
    if (!until.isAfter(holdUntil)) {
      throw new BusinessRuleException(
          "HOLD_EXTENSION_DATE", "The extension must be later than " + holdUntil);
    }
    this.requestedUntil = until;
  }

  /**
   * Applies or drops the requested extension.
   *
   * @param granted whether it was approved
   */
  public void extensionDecided(boolean granted) {
    if (granted && requestedUntil != null) {
      holdUntil = requestedUntil;
      extensionCount++;
      expiryNotified = false;
    }
    requestedUntil = null;
  }

  /**
   * Assigns the approved hold to a remittance processor (MKTID.004).
   *
   * @param processor processor
   */
  public void assign(String processor) {
    this.assignedProcessor = processor;
  }

  /** Records that the requestor and processor were told the hold expires (HOLD_EXPIRING). */
  public void expiryNotified() {
    this.expiryNotified = true;
  }

  /**
   * Records the release (MKTID.002, RMTID.021).
   *
   * @param note why
   * @param at time
   */
  public void released(String note, Instant at) {
    this.releaseNote = note;
    this.releasedAt = at;
  }

  /**
   * Mirrors the workflow stage.
   *
   * @param newStage stage
   */
  public void markStage(HoldStage newStage) {
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

  public String getReasonCode() {
    return reasonCode;
  }

  public String getRemarks() {
    return remarks;
  }

  public LocalDate getHoldUntil() {
    return holdUntil;
  }

  public LocalDate getRequestedUntil() {
    return requestedUntil;
  }

  public int getExtensionCount() {
    return extensionCount;
  }

  public HoldStage getStage() {
    return stage;
  }

  public RequestSource getSource() {
    return source;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public String getAssignedProcessor() {
    return assignedProcessor;
  }

  public RemittanceStatus getPreviousStatus() {
    return previousStatus;
  }

  public boolean isExpiryNotified() {
    return expiryNotified;
  }

  public Instant getReleasedAt() {
    return releasedAt;
  }

  public String getReleaseNote() {
    return releaseNote;
  }

  /**
   * Terms of a hold.
   *
   * @param reasonCode reason (LOV HOLD_REASON)
   * @param remarks remarks
   * @param holdUntil hold until
   */
  public record Terms(String reasonCode, String remarks, LocalDate holdUntil) {}

  /**
   * Where a request comes from.
   *
   * @param source screen or Collection feed
   * @param requestedBy requestor
   */
  public record Origin(RequestSource source, String requestedBy) {}
}
