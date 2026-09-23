package com.iortatechnxt.finverse.party.domain;

import com.iortatechnxt.finverse.coa.domain.SubLedgerType;

/** Business partner categories and the sub-ledger each belongs to. */
public enum PartyType {
  INDIVIDUAL_CLIENT(SubLedgerType.POLICYHOLDER),
  CORPORATE_CLIENT(SubLedgerType.POLICYHOLDER),
  AGENT(SubLedgerType.INTERMEDIARY),
  BROKER(SubLedgerType.INTERMEDIARY),
  REINSURER(SubLedgerType.REINSURER),
  COINSURER(SubLedgerType.COINSURER),
  SUPPLIER(SubLedgerType.VENDOR),
  GARAGE(SubLedgerType.VENDOR),
  SURVEYOR(SubLedgerType.VENDOR),
  BANK(SubLedgerType.BANK);

  private final SubLedgerType subLedger;

  PartyType(SubLedgerType subLedger) {
    this.subLedger = subLedger;
  }

  public SubLedgerType subLedger() {
    return subLedger;
  }

  /**
   * Whether the party earns commission on business it introduces.
   *
   * @return true for agents and brokers
   */
  public boolean isIntermediary() {
    return subLedger == SubLedgerType.INTERMEDIARY;
  }
}
