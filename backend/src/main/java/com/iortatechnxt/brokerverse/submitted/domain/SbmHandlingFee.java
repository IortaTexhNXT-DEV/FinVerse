package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A handling fee billed on a CBG Motor submitted account (BRIDSP-31; design section 3.6): its PN
 * (CLPC payments) or location reference (OTC payments), amount and billing date; TAGGED when a
 * payment matched and the income request went to Cashiering, APPLIED when Cashiering recognised it
 * with the official receipt.
 */
@Entity
@Table(name = "sbm_handling_fee")
public class SbmHandlingFee extends BaseEntity {

  private static final int TEXT = 500;

  /** Billed, waiting for payment. */
  public static final String BILLED = "BILLED";

  /** A payment matched. */
  public static final String TAGGED = "TAGGED";

  /** Applied with an official receipt. */
  public static final String APPLIED = "APPLIED";

  /** Cancelled by the handler. */
  public static final String CANCELLED = "CANCELLED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "fee_no", nullable = false, updatable = false, length = 30)
  private String feeNo;

  @Column(name = "policy_id")
  private Long policyId;

  @Column(name = "pn_no", length = 40)
  private String pnNo;

  @Column(name = "location_ref", length = 60)
  private String locationRef;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "billing_date", nullable = false)
  private LocalDate billingDate;

  @Column(nullable = false, length = 20)
  private String status = BILLED;

  @Column(name = "unapplied_ref", length = 30)
  private String unappliedRef;

  @Column(length = 20)
  private String channel;

  @Column(name = "ticket_ref", length = 40)
  private String ticketRef;

  @Column(name = "ticket_message", length = 500)
  private String ticketMessage;

  @Column(name = "or_no", length = 30)
  private String orNo;

  @Column(name = "tagged_by", length = 50)
  private String taggedBy;

  @Column(name = "tagged_at")
  private Instant taggedAt;

  @Column(name = "applied_at")
  private Instant appliedAt;

  @Column(name = "cancel_reason", length = 250)
  private String cancelReason;

  @Column(name = "bulk_job_no", length = 30)
  private String bulkJobNo;

  protected SbmHandlingFee() {}

  /**
   * A billed handling fee.
   *
   * @param companyId company
   * @param feeNo number
   * @param bill policy, keys, amount and billing date
   * @param bulkJobNo upload, may be null
   * @param baseCurrency base currency of the company, the currency of a bill without one
   */
  public SbmHandlingFee(
      Long companyId, String feeNo, Bill bill, String bulkJobNo, String baseCurrency) {
    this.companyId = companyId;
    this.feeNo = feeNo;
    this.policyId = bill.policyId();
    this.pnNo = bill.pnNo();
    this.locationRef = bill.locationRef();
    this.amount = bill.amount();
    this.currency = bill.currency() == null ? baseCurrency : bill.currency();
    this.billingDate = bill.billingDate();
    this.bulkJobNo = bulkJobNo;
  }

  /**
   * A payment matched: the income request is sent.
   *
   * @param ref unapplied item
   * @param paymentChannel channel of the payment
   * @param by user or SYSTEM
   * @param at time
   */
  public void tag(String ref, String paymentChannel, String by, Instant at) {
    if (!BILLED.equals(status)) {
      throw new BusinessRuleException("SBM_FEE_NOT_BILLED", feeNo + " is not waiting for payment");
    }
    this.unappliedRef = ref;
    this.channel = paymentChannel;
    this.taggedBy = by;
    this.taggedAt = at;
    this.status = TAGGED;
  }

  /**
   * The answer of Cashiering to the request.
   *
   * @param ref disposition reference
   * @param message message
   */
  public void ticket(String ref, String message) {
    this.ticketRef = ref;
    this.ticketMessage =
        message != null && message.length() > TEXT ? message.substring(0, TEXT) : message;
  }

  /**
   * Cashiering applied the payment.
   *
   * @param receipt official receipt, may be null
   * @param at time
   */
  public void applied(String receipt, Instant at) {
    this.status = APPLIED;
    this.orNo = receipt;
    this.appliedAt = at;
  }

  /**
   * Cashiering refused the request: back to billed.
   *
   * @param message reason
   */
  public void refused(String message) {
    this.status = BILLED;
    this.unappliedRef = null;
    this.channel = null;
    this.ticketMessage = message;
  }

  /**
   * Cancels a billed fee.
   *
   * @param reason reason
   */
  public void cancel(String reason) {
    if (!BILLED.equals(status)) {
      throw new BusinessRuleException("SBM_FEE_NOT_BILLED", feeNo + " is not waiting for payment");
    }
    this.status = CANCELLED;
    this.cancelReason = reason;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getFeeNo() {
    return feeNo;
  }

  public Long getPolicyId() {
    return policyId;
  }

  public String getPnNo() {
    return pnNo;
  }

  public String getLocationRef() {
    return locationRef;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public LocalDate getBillingDate() {
    return billingDate;
  }

  public String getStatus() {
    return status;
  }

  public String getUnappliedRef() {
    return unappliedRef;
  }

  public String getChannel() {
    return channel;
  }

  public String getTicketRef() {
    return ticketRef;
  }

  public String getTicketMessage() {
    return ticketMessage;
  }

  public String getOrNo() {
    return orNo;
  }

  public String getTaggedBy() {
    return taggedBy;
  }

  public Instant getTaggedAt() {
    return taggedAt;
  }

  public Instant getAppliedAt() {
    return appliedAt;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  public String getBulkJobNo() {
    return bulkJobNo;
  }

  /**
   * What is billed.
   *
   * @param policyId masterlist record, may be null
   * @param pnNo PN (CLPC), may be null
   * @param locationRef location reference (OTC), may be null
   * @param amount amount
   * @param currency currency
   * @param billingDate billing date
   */
  public record Bill(
      Long policyId,
      String pnNo,
      String locationRef,
      BigDecimal amount,
      String currency,
      LocalDate billingDate) {}
}
