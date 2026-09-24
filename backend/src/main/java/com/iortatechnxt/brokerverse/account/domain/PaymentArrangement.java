package com.iortatechnxt.brokerverse.account.domain;

/** Who collects the premium (BRNB.114). */
public enum PaymentArrangement {
  /** The client pays BDOI, which remits to the insurer. */
  VIA_BDOI,
  /** The client pays the insurer directly (direct payment). */
  DIRECT_TO_INSURER
}
