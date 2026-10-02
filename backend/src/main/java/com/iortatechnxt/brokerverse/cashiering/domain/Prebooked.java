package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A payment matched to an account that is not booked yet (CSHID.020, OQ12). The AR is issued and
 * the money waits in unapplied collections; the booking of the account (ledger event) or the {@code
 * PREBOOKED_REMATCH} job applies it and the item leaves the queue.
 */
@Entity
@Table(name = "csh_prebooked")
public class Prebooked extends BaseEntity {

  /** Waiting for the booking. */
  public static final String OPEN = "OPEN";

  /** Applied after the booking. */
  public static final String APPLIED = "APPLIED";

  /** Released to the unapplied workbench. */
  public static final String RELEASED = "RELEASED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "payment_id", nullable = false, updatable = false)
  private Long paymentId;

  @Column(name = "receipt_id", nullable = false, updatable = false)
  private Long receiptId;

  @Column(nullable = false, length = 80, updatable = false)
  private String reference;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(name = "first_seen", nullable = false, updatable = false)
  private LocalDate firstSeen;

  @Column(name = "last_rematch")
  private Instant lastRematch;

  @Column(name = "rematch_count", nullable = false)
  private int rematchCount;

  @Column(nullable = false, length = 20)
  private String status = OPEN;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "application_id")
  private Long applicationId;

  @Column(length = 250)
  private String remarks;

  protected Prebooked() {}

  /**
   * Creates an open item.
   *
   * @param payment payment
   * @param receiptId AR issued
   * @param reference reference that matched
   * @param arn pre-booked account
   */
  public Prebooked(Payment payment, Long receiptId, String reference, String arn) {
    this.companyId = payment.getCompanyId();
    this.paymentId = payment.getId();
    this.receiptId = receiptId;
    this.reference = reference;
    this.arn = arn;
    this.amount = payment.getAmount();
    this.currency = payment.getCurrency();
    this.firstSeen = payment.getValueDate();
  }

  /**
   * Records a re-match attempt that found no booking yet.
   *
   * @param at time
   */
  public void attempted(Instant at) {
    lastRematch = at;
    rematchCount++;
  }

  /**
   * Resolves the item.
   *
   * @param outcome APPLIED or RELEASED
   * @param application application, may be null
   * @param note remarks
   * @param at time
   */
  public void resolve(String outcome, Long application, String note, Instant at) {
    this.status = outcome;
    this.applicationId = application;
    this.remarks = note;
    this.resolvedAt = at;
    this.lastRematch = at;
  }

  /**
   * Whether the item still waits.
   *
   * @return true when open
   */
  public boolean isOpen() {
    return OPEN.equals(status);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getPaymentId() {
    return paymentId;
  }

  public Long getReceiptId() {
    return receiptId;
  }

  public String getReference() {
    return reference;
  }

  public String getArn() {
    return arn;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public LocalDate getFirstSeen() {
    return firstSeen;
  }

  public Instant getLastRematch() {
    return lastRematch;
  }

  public int getRematchCount() {
    return rematchCount;
  }

  public String getStatus() {
    return status;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public Long getApplicationId() {
    return applicationId;
  }

  public String getRemarks() {
    return remarks;
  }
}
