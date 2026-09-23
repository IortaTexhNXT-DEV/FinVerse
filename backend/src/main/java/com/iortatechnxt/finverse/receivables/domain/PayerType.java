package com.iortatechnxt.finverse.receivables.domain;

import com.iortatechnxt.finverse.party.domain.PartyType;
import java.util.EnumSet;
import java.util.Set;

/**
 * Who pays a receipt; drives the accounting event and whether the receipt is matched against the
 * payer's open items.
 */
public enum PayerType {
  /** Individual or corporate client: premium collection. */
  POLICYHOLDER(EnumSet.of(PartyType.INDIVIDUAL_CLIENT, PartyType.CORPORATE_CLIENT)),
  /** Agent or broker remitting premium. */
  INTERMEDIARY(EnumSet.of(PartyType.AGENT, PartyType.BROKER)),
  /** Reinsurer (or reinsurance broker) settling its balance. */
  REINSURER(EnumSet.of(PartyType.REINSURER, PartyType.RI_BROKER)),
  /** Any other income (no sub-ledger matching). */
  OTHER(EnumSet.noneOf(PartyType.class));

  private final Set<PartyType> partyTypes;

  PayerType(Set<PartyType> partyTypes) {
    this.partyTypes = partyTypes;
  }

  /**
   * Party types accepted for this payer type.
   *
   * @return party types (empty for OTHER)
   */
  public Set<PartyType> partyTypes() {
    return Set.copyOf(partyTypes);
  }

  /**
   * Whether receipts of this payer type settle open items of a sub-ledger party.
   *
   * @return true except for OTHER
   */
  public boolean hasParty() {
    return this != OTHER;
  }

  /**
   * Whether money not applied to a debit note is held in the premium deposit account.
   *
   * @return true for policyholders and intermediaries
   */
  public boolean usesPremiumDeposit() {
    return this == POLICYHOLDER || this == INTERMEDIARY;
  }

  /**
   * Party types that can owe the company money through the sub-ledger (clients, intermediaries,
   * reinsurers).
   *
   * @return party types
   */
  public static Set<PartyType> debtorPartyTypes() {
    Set<PartyType> all = EnumSet.noneOf(PartyType.class);
    for (PayerType p : values()) {
      all.addAll(p.partyTypes);
    }
    return all;
  }

  /**
   * Payer type of a party.
   *
   * @param type party type
   * @return payer type (OTHER when the party is not a debtor)
   */
  public static PayerType of(PartyType type) {
    for (PayerType p : values()) {
      if (p.partyTypes.contains(type)) {
        return p;
      }
    }
    return OTHER;
  }
}
