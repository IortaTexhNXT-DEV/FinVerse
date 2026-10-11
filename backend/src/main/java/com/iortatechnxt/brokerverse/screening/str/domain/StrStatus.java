package com.iortatechnxt.brokerverse.screening.str.domain;

/** Status of a suspicious transaction report (SNSRP-705, 706; FRS section 5.4). */
public enum StrStatus {
  /** Prefilled from the case; editable by the Compliance Officer. */
  DRAFT,
  /** Complete and marked ready; no AML Committee APPROVE_STR decision. */
  FOR_APPROVAL,
  /** The case has an AML Committee APPROVE_STR decision; eligible for extraction. */
  APPROVED,
  /** Included in an AMLC extraction file. */
  EXTRACTED,
  /** Filed on the AMLC portal; AMLC reference and filing date recorded. */
  FILED
}
