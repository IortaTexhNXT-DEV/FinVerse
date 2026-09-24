package com.iortatechnxt.brokerverse.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Lifecycle data of an account owned by the account module but written by later modules (placement,
 * issuance, booking) through {@code AccountLifecycleService}: payment gate, placement, hold cover,
 * policy issue, booking and cancellation.
 */
@Embeddable
public class AccountLifecycle {

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_status", nullable = false, length = 20)
  private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

  @Column(name = "payment_source", length = 60)
  private String paymentSource;

  @Column(name = "payment_confirmed_at")
  private Instant paymentConfirmedAt;

  @Column(name = "placement_slip_ref", length = 40)
  private String placementSlipRef;

  @Column(name = "placed_at")
  private Instant placedAt;

  @Column(name = "insurer_ref", length = 60)
  private String insurerRef;

  @Enumerated(EnumType.STRING)
  @Column(name = "hold_cover_status", length = 20)
  private HoldCoverStatus holdCoverStatus;

  @Column(name = "hold_cover_ref", length = 60)
  private String holdCoverRef;

  @Column(name = "hold_cover_date")
  private LocalDate holdCoverDate;

  @Column(name = "policy_issue_date")
  private LocalDate policyIssueDate;

  @Column(name = "epolicy_received", nullable = false)
  private boolean epolicyReceived;

  @Column(name = "booking_ref", length = 40)
  private String bookingRef;

  @Column(name = "booked_at")
  private LocalDate bookedAt;

  @Column(name = "incentive_flag", nullable = false)
  private boolean incentiveFlag;

  @Column(name = "cancelled_at")
  private LocalDate cancelledAt;

  @Column(name = "cancellation_reason", length = 300)
  private String cancellationReason;

  /**
   * Records the payment gate outcome.
   *
   * @param status paid, client confirmed or direct
   * @param source source (CLPC report, payment report, manual...)
   * @param when time
   */
  void paymentConfirmed(PaymentStatus status, String source, Instant when) {
    this.paymentStatus = status;
    this.paymentSource = source;
    this.paymentConfirmedAt = when;
  }

  void placed(String slipRef, Instant when) {
    this.placementSlipRef = slipRef;
    this.placedAt = when;
  }

  void holdCover(HoldCoverStatus status, String reference, LocalDate date) {
    this.holdCoverStatus = status;
    this.holdCoverRef = reference;
    this.holdCoverDate = date;
    if (reference != null) {
      this.insurerRef = reference;
    }
  }

  void policyIssued(LocalDate issueDate, boolean epolicy) {
    this.policyIssueDate = issueDate;
    this.epolicyReceived = epolicy;
  }

  void booked(String reference, LocalDate date, boolean incentive) {
    this.bookingRef = reference;
    this.bookedAt = date;
    this.incentiveFlag = incentive;
  }

  void cancelled(LocalDate date, String reason) {
    this.cancelledAt = date;
    this.cancellationReason = reason;
  }

  public PaymentStatus getPaymentStatus() {
    return paymentStatus;
  }

  public String getPaymentSource() {
    return paymentSource;
  }

  public Instant getPaymentConfirmedAt() {
    return paymentConfirmedAt;
  }

  public String getPlacementSlipRef() {
    return placementSlipRef;
  }

  public Instant getPlacedAt() {
    return placedAt;
  }

  public String getInsurerRef() {
    return insurerRef;
  }

  public HoldCoverStatus getHoldCoverStatus() {
    return holdCoverStatus;
  }

  public String getHoldCoverRef() {
    return holdCoverRef;
  }

  public LocalDate getHoldCoverDate() {
    return holdCoverDate;
  }

  public LocalDate getPolicyIssueDate() {
    return policyIssueDate;
  }

  public boolean isEpolicyReceived() {
    return epolicyReceived;
  }

  public String getBookingRef() {
    return bookingRef;
  }

  public LocalDate getBookedAt() {
    return bookedAt;
  }

  public boolean isIncentiveFlag() {
    return incentiveFlag;
  }

  public LocalDate getCancelledAt() {
    return cancelledAt;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }
}
