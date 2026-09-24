package com.iortatechnxt.brokerverse.account.domain;

/** Hold cover requested from the insurer while the placement completes (BRNB.072/103). */
public enum HoldCoverStatus {
  /** Requested from the insurer. */
  REQUESTED,
  /** Confirmed by the insurer. */
  CONFIRMED,
  /** Declined by the insurer. */
  DECLINED,
  /** Lapsed without a policy. */
  EXPIRED
}
