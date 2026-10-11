package com.iortatechnxt.brokerverse.csf.domain;

/** What an agent did in the Customer Servicing Facility (activity log, FR-CSF-042). */
public enum ActivityAction {
  /** Customer search, with its criteria. */
  SEARCH,
  /** Servicing View opened. */
  VIEW,
  /** Document downloaded or ZIP of documents. */
  DOWNLOAD,
  /** Renewal advice resent. */
  RESEND_RA,
  /** E-policy resent. */
  RESEND_EPOLICY,
  /** Document uploaded. */
  UPLOAD,
  /** Caller verification recorded. */
  VERIFY,
  /** Contact change applied or refused. */
  CONTACT_CHANGE,
  /** Change referred to the fulfilment unit. */
  REFERRAL
}
