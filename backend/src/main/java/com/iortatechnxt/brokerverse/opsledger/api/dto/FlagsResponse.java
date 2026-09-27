package com.iortatechnxt.brokerverse.opsledger.api.dto;

import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import java.time.Instant;

/**
 * Flags and lock of an invoice (status chips, RMTID.032/040).
 *
 * @param directPayment direct payment (MKTID.011)
 * @param cwt2Percent client withholds 2% (CSHID.020)
 * @param incentiveEligible incentive eligible
 * @param hold on hold
 * @param pendingNegativeAdjustment negative adjustment pending
 * @param writtenOff written off
 * @param cancelled cancelled
 * @param estimated estimated
 * @param lockOwner module holding the lock, null when unlocked
 * @param lockReason lock reason
 * @param lockedBy user who locked
 * @param lockedAt lock time
 */
public record FlagsResponse(
    boolean directPayment,
    boolean cwt2Percent,
    boolean incentiveEligible,
    boolean hold,
    boolean pendingNegativeAdjustment,
    boolean writtenOff,
    boolean cancelled,
    boolean estimated,
    String lockOwner,
    String lockReason,
    String lockedBy,
    Instant lockedAt) {

  /**
   * Maps the flags of an invoice.
   *
   * @param i invoice
   * @return response
   */
  public static FlagsResponse from(OpsInvoice i) {
    return new FlagsResponse(
        i.isDpFlag(),
        i.isCwtFlag(),
        i.isIncentiveEligible(),
        i.isHoldFlag(),
        i.isPendingNegAdj(),
        i.isWrittenOff(),
        i.isCancelled(),
        i.isEstimated(),
        i.getLockOwner(),
        i.getLockReason(),
        i.getLockedBy(),
        i.getLockedAt());
  }
}
