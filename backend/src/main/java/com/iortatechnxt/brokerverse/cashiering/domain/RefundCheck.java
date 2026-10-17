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
 * A refund validation Cashiering answers (MKT 1.11.0): for the refund of a cancelled account,
 * Cashiering confirms that the premium is back in the unapplied list with a new AR number, or
 * rejects it. Opened through the port {@code RefundValidationSource} (validator CASHIERING).
 */
@Entity
@Table(name = "csh_refund_validation")
public class RefundCheck extends BaseEntity {

  /** Where a validation stands. */
  public enum Status {
    /** Waiting for a cashier. */
    OPEN,
    /** Confirmed: the premium is in the unapplied list. */
    CONFIRMED,
    /** Rejected. */
    REJECTED
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "task_no", nullable = false, length = 30, updatable = false)
  private String taskNo;

  @Column(name = "source_module", nullable = false, length = 30, updatable = false)
  private String sourceModule;

  @Column(name = "source_ref", nullable = false, length = 80, updatable = false)
  private String sourceRef;

  @Column(name = "invoice_no", length = 40, updatable = false)
  private String invoiceNo;

  @Column(name = "ar_no", length = 40, updatable = false)
  private String arNo;

  @Column(name = "client_code", length = 30, updatable = false)
  private String clientCode;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(name = "requested_by", nullable = false, length = 50, updatable = false)
  private String requestedBy;

  @Column(name = "request_remarks", length = 1000, updatable = false)
  private String requestRemarks;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private Status status = Status.OPEN;

  @Column(name = "unapplied_id")
  private Long unappliedId;

  @Column(name = "new_ar_no", length = 40)
  private String newArNo;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "result_remarks", length = 1000)
  private String resultRemarks;

  protected RefundCheck() {}

  /**
   * Opens a validation.
   *
   * @param taskNo RVL- number
   * @param spec what to validate
   */
  public RefundCheck(String taskNo, Spec spec) {
    this.companyId = spec.companyId();
    this.taskNo = taskNo;
    this.sourceModule = spec.sourceModule();
    this.sourceRef = spec.sourceRef();
    this.invoiceNo = spec.invoiceNo();
    this.arNo = spec.arNo();
    this.clientCode = spec.clientCode();
    this.currency = spec.currency();
    this.amount = spec.amount();
    this.requestedBy = spec.requestedBy();
    this.requestRemarks = spec.remarks();
  }

  /**
   * Records the answer.
   *
   * @param answer confirmed or rejected, with the unapplied item and new AR number
   * @param by cashier
   * @param at time
   */
  public void decide(Answer answer, String by, Instant at) {
    this.status = answer.confirmed() ? Status.CONFIRMED : Status.REJECTED;
    this.unappliedId = answer.unappliedId();
    this.newArNo = answer.newArNo();
    this.resultRemarks = answer.remarks();
    this.decidedBy = by;
    this.decidedAt = at;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getTaskNo() {
    return taskNo;
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

  public String getArNo() {
    return arNo;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public String getRequestRemarks() {
    return requestRemarks;
  }

  public Status getStatus() {
    return status;
  }

  public Long getUnappliedId() {
    return unappliedId;
  }

  public String getNewArNo() {
    return newArNo;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getResultRemarks() {
    return resultRemarks;
  }

  /**
   * What to validate.
   *
   * @param companyId company
   * @param sourceModule requesting module
   * @param sourceRef its reference
   * @param invoiceNo cancelled invoice
   * @param arNo AR of the payment to refund
   * @param clientCode client
   * @param currency currency
   * @param amount refund amount
   * @param requestedBy user
   * @param remarks remarks, may be null
   */
  public record Spec(
      Long companyId,
      String sourceModule,
      String sourceRef,
      String invoiceNo,
      String arNo,
      String clientCode,
      String currency,
      BigDecimal amount,
      String requestedBy,
      String remarks) {}

  /**
   * A cashier's answer.
   *
   * @param confirmed true when confirmed
   * @param unappliedId unapplied item holding the premium, may be null
   * @param newArNo new AR number, may be null
   * @param remarks remarks, may be null
   */
  public record Answer(boolean confirmed, Long unappliedId, String newArNo, String remarks) {}
}
