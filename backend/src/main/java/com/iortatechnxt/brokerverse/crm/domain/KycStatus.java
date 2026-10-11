package com.iortatechnxt.brokerverse.crm.domain;

/** Know-your-customer verification status. */
public enum KycStatus {
  /** No KYC documents yet. */
  NOT_STARTED,
  /** Documents received, waiting for verification. */
  PENDING,
  /** Verified. */
  VERIFIED,
  /** Review date passed; a periodic review is due (BRNB.110). */
  EXPIRED
}
