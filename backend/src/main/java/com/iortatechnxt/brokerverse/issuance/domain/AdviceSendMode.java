package com.iortatechnxt.brokerverse.issuance.domain;

/** How an Insurance Advice was last sent (FR-NB-107). */
public enum AdviceSendMode {
  /** Sent from the register by a user. */
  MANUAL,
  /** Sent by the system to the recipient enrolled for the mortgagee bank. */
  AUTOMATIC
}
