package com.iortatechnxt.brokerverse.opsledger.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

/**
 * A payment request in the in-app Disbursement queue, the default {@code DisbursementGateway} until
 * the Disbursement system and BRD are known (OQ02): remittances to insurers (RMTID.011), refunds
 * (CSHID.024), BIR 2307 reports (DBMID.001) and incentive pass-on (CMRID.006). Status: SENT,
 * ACKNOWLEDGED, DV_ASSIGNED, PAID or RETURNED.
 */
@Entity
@Table(name = "ops_disbursement_request")
public class DisbursementRequest extends BaseEntity {

  private static final int MAX_REASON = 250;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "request_no", nullable = false, length = 30, updatable = false)
  private String requestNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "request_type", nullable = false, length = 20, updatable = false)
  private Type requestType;

  @Column(name = "source_module", nullable = false, length = 30, updatable = false)
  private String sourceModule;

  @Column(name = "source_ref", nullable = false, length = 80, updatable = false)
  private String sourceRef;

  @Column(name = "payee_code", nullable = false, length = 30, updatable = false)
  private String payeeCode;

  @Column(name = "payee_name", length = 250, updatable = false)
  private String payeeName;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(length = 500, updatable = false)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.SENT;

  @Column(name = "dv_no", length = 40)
  private String dvNo;

  @Column(name = "sent_at", nullable = false, updatable = false)
  private Instant sentAt;

  @Column(name = "acknowledged_at")
  private Instant acknowledgedAt;

  @Column(name = "dv_assigned_at")
  private Instant dvAssignedAt;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Column(name = "returned_at")
  private Instant returnedAt;

  @Column(name = "return_reason", length = MAX_REASON)
  private String returnReason;

  @Column(name = "attachment_ref", length = 80, updatable = false)
  private String attachmentRef;

  protected DisbursementRequest() {}

  /**
   * A request sent to the queue.
   *
   * @param companyId company
   * @param requestNo request number
   * @param spec what to pay
   * @param at time
   */
  public DisbursementRequest(Long companyId, String requestNo, Spec spec, Instant at) {
    this.companyId = companyId;
    this.requestNo = requestNo;
    this.requestType = spec.type();
    this.sourceModule = spec.sourceModule();
    this.sourceRef = spec.sourceRef();
    this.payeeCode = spec.payeeCode();
    this.payeeName = spec.payeeName();
    this.currency = spec.currency();
    this.amount = spec.amount();
    this.description = spec.description();
    this.attachmentRef = spec.attachmentRef();
    this.sentAt = at;
  }

  /**
   * Disbursement acknowledges receipt.
   *
   * @param at time
   */
  public void acknowledge(Instant at) {
    requireIn(Set.of(Status.SENT), "acknowledged");
    status = Status.ACKNOWLEDGED;
    acknowledgedAt = at;
  }

  /**
   * Disbursement assigns the disbursement voucher number (RMTID.019).
   *
   * @param number DV number
   * @param at time
   */
  public void assignDv(String number, Instant at) {
    requireIn(Set.of(Status.SENT, Status.ACKNOWLEDGED), "given a DV number");
    if (acknowledgedAt == null) {
      acknowledgedAt = at;
    }
    status = Status.DV_ASSIGNED;
    dvNo = number;
    dvAssignedAt = at;
  }

  /**
   * The payment was released.
   *
   * @param at time
   */
  public void markPaid(Instant at) {
    requireIn(Set.of(Status.DV_ASSIGNED), "paid");
    status = Status.PAID;
    paidAt = at;
  }

  /**
   * Disbursement returns the request to its source with a reason.
   *
   * @param reason reason
   * @param at time
   */
  public void returnToSource(String reason, Instant at) {
    requireIn(Set.of(Status.SENT, Status.ACKNOWLEDGED, Status.DV_ASSIGNED), "returned");
    status = Status.RETURNED;
    returnReason = reason.length() <= MAX_REASON ? reason : reason.substring(0, MAX_REASON);
    returnedAt = at;
  }

  private void requireIn(Set<Status> allowed, String action) {
    if (!allowed.contains(status)) {
      throw new BusinessRuleException(
          "DISBURSEMENT_STATUS",
          "Payment request " + requestNo + " is " + status + " and cannot be " + action);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public Type getRequestType() {
    return requestType;
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public String getPayeeCode() {
    return payeeCode;
  }

  public String getPayeeName() {
    return payeeName;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getDescription() {
    return description;
  }

  public Status getStatus() {
    return status;
  }

  public String getDvNo() {
    return dvNo;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public Instant getAcknowledgedAt() {
    return acknowledgedAt;
  }

  public Instant getDvAssignedAt() {
    return dvAssignedAt;
  }

  public Instant getPaidAt() {
    return paidAt;
  }

  public Instant getReturnedAt() {
    return returnedAt;
  }

  public String getReturnReason() {
    return returnReason;
  }

  public String getAttachmentRef() {
    return attachmentRef;
  }

  /** What is paid. */
  public enum Type {
    /** Remittance to an insurer (RMTID.011). */
    REMITTANCE,
    /** Refund to a client (CSHID.024). */
    REFUND,
    /** BIR 2307 report and certificates to release to insurers (DBMID.001). */
    CWT2307,
    /** Incentive pass-on to branches (CMRID.006). */
    PASS_ON
  }

  /** Status in the Disbursement queue. */
  public enum Status {
    /** Sent to Disbursement. */
    SENT,
    /** Received by Disbursement. */
    ACKNOWLEDGED,
    /** Disbursement voucher number assigned. */
    DV_ASSIGNED,
    /** Payment released. */
    PAID,
    /** Returned to the source module. */
    RETURNED
  }

  /**
   * A payment request to create.
   *
   * @param type request type
   * @param sourceModule source module
   * @param sourceRef source reference (unique per module)
   * @param payeeCode payee party code
   * @param payeeName payee name
   * @param currency currency
   * @param amount positive amount
   * @param description description
   * @param attachmentRef supporting document (attachment or extract id), may be null
   */
  public record Spec(
      Type type,
      String sourceModule,
      String sourceRef,
      String payeeCode,
      String payeeName,
      String currency,
      BigDecimal amount,
      String description,
      String attachmentRef) {}
}
