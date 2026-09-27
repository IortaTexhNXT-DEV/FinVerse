package com.iortatechnxt.brokerverse.collections.unapplied.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A request sent to Cashiering on a collector disposition (BRCLXN.030/032): apply the unapplied
 * payment to an invoice, refund, reclass or transfer it, through the port {@code
 * UnappliedDispositionRequests}. Its status follows Cashiering's answers ({@code
 * UnappliedDispositionChanged}); the p.60 payment fields are kept as a snapshot for the daily "For
 * Application To Invoice" file (BRCLXN.041/042), which records its run on the request.
 */
@Entity
@Table(name = "clx_application_request")
public class ApplicationRequest extends BaseEntity {

  /** Where a request stands. */
  public enum Status {
    /** Queued in Cashiering. */
    SENT,
    /** No Cashiering adapter: handed over to be done by hand. */
    DEFERRED,
    /** Cashiering assigned its disposition. */
    ACCEPTED,
    /** Refused by Cashiering. */
    REJECTED,
    /** Executed by Cashiering. */
    APPLIED;

    /**
     * Whether Cashiering may still answer.
     *
     * @return true while sent, deferred or accepted
     */
    public boolean isOpen() {
      return this == SENT || this == DEFERRED || this == ACCEPTED;
    }
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "unapplied_ref", nullable = false, length = 30, updatable = false)
  private String unappliedRef;

  @Column(nullable = false, length = 20, updatable = false)
  private String action;

  @Column(name = "invoice_no", length = 40, updatable = false)
  private String invoiceNo;

  @Column(precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(name = "requested_by", nullable = false, length = 50, updatable = false)
  private String requestedBy;

  @Column(name = "requested_at", nullable = false, updatable = false)
  private Instant requestedAt;

  @Column(name = "source_ref", nullable = false, length = 80, updatable = false)
  private String sourceRef;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private Status status = Status.SENT;

  @Column(name = "cashiering_ref", length = 40)
  private String cashieringRef;

  @Column(name = "status_message", length = 1000)
  private String statusMessage;

  @Column(name = "status_at")
  private Instant statusAt;

  @Embedded private PaymentSnapshot payment;

  @Column(name = "file_run_no", length = 80)
  private String fileRunNo;

  @Column(name = "filed_at")
  private Instant filedAt;

  protected ApplicationRequest() {}

  /**
   * Records a request before it is sent.
   *
   * @param companyId company
   * @param spec item, action, invoice, amount, requester and source reference
   * @param payment payment fields of the item at request time
   */
  public ApplicationRequest(Long companyId, Spec spec, PaymentSnapshot payment) {
    this.companyId = companyId;
    this.unappliedRef = spec.unappliedRef();
    this.action = spec.action();
    this.invoiceNo = spec.invoiceNo();
    this.amount = spec.amount();
    this.requestedBy = spec.requestedBy();
    this.requestedAt = spec.requestedAt();
    this.sourceRef = spec.sourceRef();
    this.payment = payment;
  }

  /**
   * Records Cashiering's answer.
   *
   * @param next status
   * @param reference Cashiering or hand-off reference, null keeps the current one
   * @param message message
   * @param at time
   */
  public void answer(Status next, String reference, String message, Instant at) {
    this.status = next;
    if (reference != null) {
      this.cashieringRef = reference;
    }
    this.statusMessage = message;
    this.statusAt = at;
  }

  /**
   * Records the file run that listed the request.
   *
   * @param runNo file reference
   * @param at time
   */
  public void filed(String runNo, Instant at) {
    this.fileRunNo = runNo;
    this.filedAt = at;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getUnappliedRef() {
    return unappliedRef;
  }

  public String getAction() {
    return action;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public Status getStatus() {
    return status;
  }

  public String getCashieringRef() {
    return cashieringRef;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public Instant getStatusAt() {
    return statusAt;
  }

  /**
   * The payment fields at request time.
   *
   * @return snapshot (empty fields when the item was not found)
   */
  public PaymentSnapshot getPayment() {
    return payment == null ? PaymentSnapshot.EMPTY : payment;
  }

  public String getFileRunNo() {
    return fileRunNo;
  }

  public Instant getFiledAt() {
    return filedAt;
  }

  /**
   * What a request asks.
   *
   * @param unappliedRef item reference
   * @param action APPLY_TO_INVOICE, REFUND, RECLASS or TRANSFER
   * @param invoiceNo target invoice, may be null
   * @param amount amount, null for the whole balance
   * @param requestedBy collector
   * @param requestedAt time
   * @param sourceRef idempotency key sent to Cashiering
   */
  public record Spec(
      String unappliedRef,
      String action,
      String invoiceNo,
      BigDecimal amount,
      String requestedBy,
      Instant requestedAt,
      String sourceRef) {}
}
