package com.iortatechnxt.brokerverse.acsl.domain;

/** The sub-ledger compared with a control account in the GL-SL reconciliation (ACSL 2.13.2). */
public enum SlSource {
  /** The account's postings that carry a sub-ledger party (default of every control account). */
  PARTY_LEDGER,
  /** Outstanding open items of the configured document types. */
  OPEN_ITEMS,
  /** Balance of the configured Operations invoice ledger components. */
  OPS_LEDGER
}
