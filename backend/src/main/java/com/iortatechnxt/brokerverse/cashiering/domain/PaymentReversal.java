package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A sub-ledger payment reversal requested by another module (ACSL 2.6.0-2.6.1) through the port
 * {@code PaymentReversalRequester}: a cashiering approver reverses the receipt's applications on
 * the invoice (payment back to unapplied collections) or rejects the request.
 */
@Entity
@Table(name = "csh_payment_reversal")
public class PaymentReversal extends BaseEntity {

  /** Where a reversal request stands. */
  public enum Status {
    /** Waiting for approval. */
    SUBMITTED,
    /** Approved and posted. */
    APPROVED,
    /** Rejected. */
    REJECTED
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "request_no", nullable = false, length = 30, updatable = false)
  private String requestNo;

  @Column(name = "source_module", nullable = false, length = 30, updatable = false)
  private String sourceModule;

  @Column(name = "source_ref", nullable = false, length = 80, updatable = false)
  private String sourceRef;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(name = "receipt_no", nullable = false, length = 40, updatable = false)
  private String receiptNo;

  @Column(length = 3, updatable = false)
  private String currency;

  @Column(precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(name = "value_date", nullable = false, updatable = false)
  private LocalDate valueDate;

  @Column(length = 1000, updatable = false)
  private String reason;

  @Column(name = "requested_by", nullable = false, length = 50, updatable = false)
  private String requestedBy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private Status status = Status.SUBMITTED;

  @Column(name = "reversed_amount", precision = 19, scale = 2)
  private BigDecimal reversedAmount;

  @Column(name = "unapplied_id")
  private Long unappliedId;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_note", length = 1000)
  private String decisionNote;

  protected PaymentReversal() {}

  /**
   * Records a request.
   *
   * @param requestNo PRV- number
   * @param spec what to reverse
   */
  public PaymentReversal(String requestNo, Spec spec) {
    this.companyId = spec.companyId();
    this.requestNo = requestNo;
    this.sourceModule = spec.sourceModule();
    this.sourceRef = spec.sourceRef();
    this.invoiceNo = spec.invoiceNo();
    this.receiptNo = spec.receiptNo();
    this.currency = spec.currency();
    this.amount = spec.amount();
    this.valueDate = spec.valueDate();
    this.reason = spec.reason();
    this.requestedBy = spec.requestedBy();
  }

  /**
   * Approved and posted.
   *
   * @param reversed amount taken off the invoice
   * @param unapplied unapplied item holding the money
   * @param by approver
   * @param at time
   */
  public void approve(BigDecimal reversed, Long unapplied, String by, Instant at) {
    this.status = Status.APPROVED;
    this.reversedAmount = reversed;
    this.unappliedId = unapplied;
    this.decidedBy = by;
    this.decidedAt = at;
  }

  /**
   * Rejected.
   *
   * @param note reason
   * @param by approver
   * @param at time
   */
  public void reject(String note, String by, Instant at) {
    this.status = Status.REJECTED;
    this.decisionNote = note;
    this.decidedBy = by;
    this.decidedAt = at;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getReceiptNo() {
    return receiptNo;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public LocalDate getValueDate() {
    return valueDate;
  }

  public String getReason() {
    return reason;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public Status getStatus() {
    return status;
  }

  public BigDecimal getReversedAmount() {
    return reversedAmount;
  }

  public Long getUnappliedId() {
    return unappliedId;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getDecisionNote() {
    return decisionNote;
  }

  /**
   * What to reverse.
   *
   * @param companyId company
   * @param sourceModule requesting module
   * @param sourceRef its reference
   * @param invoiceNo invoice
   * @param receiptNo receipt of the payment
   * @param currency currency, may be null
   * @param amount amount, null for the whole application
   * @param valueDate value date
   * @param reason reason
   * @param requestedBy user
   */
  public record Spec(
      Long companyId,
      String sourceModule,
      String sourceRef,
      String invoiceNo,
      String receiptNo,
      String currency,
      BigDecimal amount,
      LocalDate valueDate,
      String reason,
      String requestedBy) {}
}
