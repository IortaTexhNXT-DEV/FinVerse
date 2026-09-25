package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A collector's request to dispose of an unapplied item (BRCLXN.030-033), received through the
 * Operations port {@code UnappliedDispositionRequests}: apply to an invoice, refund, reclass or
 * transfer. It waits in the Cashiering queue until a cashier accepts it (a disposition of the
 * workflow {@code OPS_DISPOSITION} is assigned from it) or rejects it; the executed disposition
 * marks it applied.
 */
@Entity
@Table(name = "csh_collector_request")
public class CollectorRequest extends BaseEntity {

  /** Where a request stands. */
  public enum Status {
    /** Waiting for a cashier. */
    QUEUED,
    /** A cashiering disposition was assigned from it. */
    ACCEPTED,
    /** Refused, or its disposition was withdrawn. */
    REJECTED,
    /** Its disposition was executed. */
    APPLIED
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "request_no", nullable = false, length = 30, updatable = false)
  private String requestNo;

  @Column(name = "unapplied_id", nullable = false, updatable = false)
  private Long unappliedId;

  @Column(nullable = false, length = 20, updatable = false)
  private String action;

  @Column(name = "invoice_no", length = 40, updatable = false)
  private String invoiceNo;

  @Column(precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(name = "requested_by", nullable = false, length = 50, updatable = false)
  private String requestedBy;

  @Column(nullable = false, length = 30, updatable = false)
  private String source;

  @Column(name = "source_ref", nullable = false, length = 80, updatable = false)
  private String sourceRef;

  @Column(length = 1000, updatable = false)
  private String remarks;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private Status status = Status.QUEUED;

  @Column(name = "disposition_id")
  private Long dispositionId;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_note", length = 1000)
  private String decisionNote;

  protected CollectorRequest() {}

  /**
   * Queues a request.
   *
   * @param requestNo CRQ- number
   * @param unappliedId unapplied item
   * @param spec what the collector asks
   */
  public CollectorRequest(String requestNo, Long unappliedId, Spec spec) {
    this.companyId = spec.companyId();
    this.requestNo = requestNo;
    this.unappliedId = unappliedId;
    this.action = spec.action();
    this.invoiceNo = spec.invoiceNo();
    this.amount = spec.amount();
    this.requestedBy = spec.requestedBy();
    this.source = spec.source();
    this.sourceRef = spec.sourceRef();
    this.remarks = spec.remarks();
  }

  /**
   * Accepted: a disposition was assigned from the request.
   *
   * @param disposition disposition id
   * @param by cashier
   * @param at time
   */
  public void accept(Long disposition, String by, Instant at) {
    this.dispositionId = disposition;
    decide(Status.ACCEPTED, by, at, null);
  }

  /**
   * Rejected by a cashier, or its disposition withdrawn.
   *
   * @param by cashier
   * @param at time
   * @param note reason
   */
  public void reject(String by, Instant at, String note) {
    decide(Status.REJECTED, by, at, note);
  }

  /**
   * The disposition was executed.
   *
   * @param at time
   * @param note what was done
   */
  public void applied(Instant at, String note) {
    this.status = Status.APPLIED;
    this.decidedAt = at;
    this.decisionNote = note;
  }

  private void decide(Status next, String by, Instant at, String note) {
    this.status = next;
    this.decidedBy = by;
    this.decidedAt = at;
    this.decisionNote = note;
  }

  /**
   * Whether a cashier can still act on the request.
   *
   * @return true while queued
   */
  public boolean isQueued() {
    return status == Status.QUEUED;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public Long getUnappliedId() {
    return unappliedId;
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

  public String getSource() {
    return source;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public String getRemarks() {
    return remarks;
  }

  public Status getStatus() {
    return status;
  }

  public Long getDispositionId() {
    return dispositionId;
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
   * What a collector asks.
   *
   * @param companyId company
   * @param action APPLY_TO_INVOICE, REFUND, RECLASS or TRANSFER
   * @param invoiceNo target invoice, may be null
   * @param amount amount, null for the whole balance
   * @param requestedBy collector
   * @param source requesting module
   * @param sourceRef its reference
   * @param remarks remarks, may be null
   */
  public record Spec(
      Long companyId,
      String action,
      String invoiceNo,
      BigDecimal amount,
      String requestedBy,
      String source,
      String sourceRef,
      String remarks) {}
}
