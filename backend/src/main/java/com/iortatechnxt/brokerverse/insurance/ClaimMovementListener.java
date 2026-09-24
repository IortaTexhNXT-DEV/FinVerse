package com.iortatechnxt.brokerverse.insurance;

/**
 * PORT implemented by modules that react to claim movements (reinsurance: recoveries and the
 * reinsurers' share of reserves).
 *
 * <p>Claims calls every listener bean synchronously, inside the transaction that posted the
 * movement, after the claims journal was posted. A listener failure therefore rolls the claims
 * transaction back, which keeps the gross and the ceded books consistent.
 *
 * <p>Implementation contract: idempotent on {@link ClaimMovement#reference()}; must not call back
 * into the claims module.
 */
public interface ClaimMovementListener {

  /**
   * Handles a posted claim movement.
   *
   * @param movement the movement
   */
  void onClaimMovement(ClaimMovement movement);
}
