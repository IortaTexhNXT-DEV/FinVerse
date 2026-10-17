package com.iortatechnxt.brokerverse.renewal.domain;

/** Where a renewal candidate comes from (RENEWAL_DESIGN section 4.1). */
public enum CandidateSource {
  /** A booked BIBS root invoice (Operations ledger). */
  BIBS_INVOICE,
  /** A submitted policy handed over by Submitted Policies. */
  SUBMITTED_POLICY,
  /** A policy migrated from the legacy systems (go-live window and later expiries). */
  LEGACY;
}
