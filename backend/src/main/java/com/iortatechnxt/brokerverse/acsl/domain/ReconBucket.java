package com.iortatechnxt.brokerverse.acsl.domain;

/** Where an insurer SOA line stands in BDOI's books (ACSL 2.14.1). */
public enum ReconBucket {
  /** The client has not paid the premium in full. */
  OUTSTANDING,
  /** Collected and due to the insurer, not yet (fully) remitted. */
  FOR_REMITTANCE,
  /** Remitted to the insurer. */
  REMITTED,
  /** Cancelled in BDOI's books. */
  CANCELLED,
  /** Direct billed: paid by the client to the insurer. */
  DIRECT_BILLED,
  /** Not booked by BDOI. */
  NOT_FOUND
}
