package com.iortatechnxt.brokerverse.issuance.domain;

/** How a received e-policy was matched to its account (BRNB.073). */
public enum MatchMethod {
  /** Chosen by the user. */
  MANUAL,
  /** The policy number given with the upload. */
  POLICY_NUMBER,
  /** The ARN in the file name. */
  FILE_NAME,
  /** The ARN printed in the document. */
  CONTENT
}
