package com.iortatechnxt.brokerverse.account.api.dto;

import com.iortatechnxt.brokerverse.account.domain.AccountLifecycle;
import com.iortatechnxt.brokerverse.account.domain.HoldCoverStatus;
import com.iortatechnxt.brokerverse.account.domain.PaymentStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Lifecycle data of an account written by placement, issuance and booking.
 *
 * @param paymentStatus payment gate status
 * @param paymentSource payment source
 * @param paymentConfirmedAt payment confirmation time
 * @param placementSlipRef placement slip
 * @param placedAt placement time
 * @param insurerRef insurer reference
 * @param holdCoverStatus hold cover status
 * @param holdCoverRef hold cover reference
 * @param holdCoverDate hold cover date
 * @param policyIssueDate policy issue date
 * @param epolicyReceived e-policy received
 * @param bookingRef booking reference
 * @param bookedAt booking date
 * @param incentiveFlag incentive flag
 * @param cancelledAt cancellation date
 * @param cancellationReason cancellation reason
 */
public record LifecycleResponse(
    PaymentStatus paymentStatus,
    String paymentSource,
    Instant paymentConfirmedAt,
    String placementSlipRef,
    Instant placedAt,
    String insurerRef,
    HoldCoverStatus holdCoverStatus,
    String holdCoverRef,
    LocalDate holdCoverDate,
    LocalDate policyIssueDate,
    boolean epolicyReceived,
    String bookingRef,
    LocalDate bookedAt,
    boolean incentiveFlag,
    LocalDate cancelledAt,
    String cancellationReason) {

  /**
   * Maps the lifecycle.
   *
   * @param l lifecycle
   * @return response
   */
  public static LifecycleResponse from(AccountLifecycle l) {
    return new LifecycleResponse(
        l.getPaymentStatus(),
        l.getPaymentSource(),
        l.getPaymentConfirmedAt(),
        l.getPlacementSlipRef(),
        l.getPlacedAt(),
        l.getInsurerRef(),
        l.getHoldCoverStatus(),
        l.getHoldCoverRef(),
        l.getHoldCoverDate(),
        l.getPolicyIssueDate(),
        l.isEpolicyReceived(),
        l.getBookingRef(),
        l.getBookedAt(),
        l.isIncentiveFlag(),
        l.getCancelledAt(),
        l.getCancellationReason());
  }
}
