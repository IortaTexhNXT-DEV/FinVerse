package com.iortatechnxt.brokerverse.screening.matching.domain;

/** What started a screening run (SNSRP-602, 303; FR-SS-030; design 4.3). */
public enum ScreeningTrigger {
  /** A client or prospect was created ({@code crm.service.ClientRegistered}). */
  CLIENT_REGISTERED,
  /** A client's name, birth date, nationality, TIN or ID changed. */
  CLIENT_CHANGED,
  /** An account of the client was submitted to Processing. */
  ACCOUNT_SUBMITTED,
  /** Watchlist entries were added, changed or delisted (delta screening). */
  LIST_CHANGE,
  /** The batch window ({@code SCR_PERIODIC_SCREENING}). */
  PERIODIC,
  /** Started by a user. */
  MANUAL
}
