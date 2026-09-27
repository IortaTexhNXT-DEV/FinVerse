package com.iortatechnxt.brokerverse.claims.domain;

import com.iortatechnxt.brokerverse.party.domain.PartyType;
import java.util.EnumSet;
import java.util.Set;

/** Role of a business partner involved in a claim, with the party types that may hold it. */
public enum ClaimPartyRole {
  /** Person or company claiming under the policy (usually the policyholder). */
  CLAIMANT(EnumSet.of(PartyType.INDIVIDUAL_CLIENT, PartyType.CORPORATE_CLIENT)),
  /** Third party claiming against the insured (liability, motor TP). */
  THIRD_PARTY(EnumSet.of(PartyType.INDIVIDUAL_CLIENT, PartyType.CORPORATE_CLIENT)),
  /** Garage repairing a vehicle (LPO holder). */
  GARAGE(EnumSet.of(PartyType.GARAGE)),
  /** Surveyor assessing the loss. */
  SURVEYOR(EnumSet.of(PartyType.SURVEYOR)),
  /** Loss adjuster (registered as a surveyor-type party). */
  ADJUSTER(EnumSet.of(PartyType.SURVEYOR));

  private final Set<PartyType> partyTypes;

  ClaimPartyRole(Set<PartyType> partyTypes) {
    this.partyTypes = partyTypes;
  }

  /**
   * Party types allowed for the role.
   *
   * @return party types
   */
  public Set<PartyType> partyTypes() {
    return EnumSet.copyOf(partyTypes);
  }
}
