package com.iortatechnxt.brokerverse.booking.service.port;

/**
 * Port to the client acceptance of a renewal (BRRN.041; FR-RN-087): a renewal is evaluated for the
 * incentive only once the acceptance of the client is confirmed through the Account Officer.
 * Renewal implements it; without an implementation every account counts as accepted.
 */
public interface IncentiveAcceptanceGate {

  /**
   * Whether the acceptance of the account is confirmed (always true for New Business).
   *
   * @param arn account
   * @return true when the incentive may be evaluated
   */
  boolean acceptanceConfirmed(String arn);
}
