package com.iortatechnxt.finverse.claims.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.party.domain.Party;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

/** A business partner involved in a claim (claimant, third party, garage, surveyor, adjuster). */
@Entity
@Table(name = "clm_claim_party")
public class ClaimParty extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "claim_id")
  private Claim claim;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "party_id")
  private Party party;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ClaimPartyRole role;

  protected ClaimParty() {}

  private ClaimParty(Claim claim, Party party, ClaimPartyRole role) {
    this.claim = claim;
    this.party = party;
    this.role = role;
  }

  /**
   * Involves a party in a claim.
   *
   * @param claim claim
   * @param party party
   * @param role role
   * @return involvement
   * @throws BusinessRuleException when the party type does not fit the role
   */
  static ClaimParty of(Claim claim, Party party, ClaimPartyRole role) {
    if (!role.partyTypes().contains(party.getPartyType())) {
      throw new BusinessRuleException(
          "INVALID_CLAIM_PARTY",
          party.getCode() + " (" + party.getPartyType() + ") cannot act as " + role);
    }
    return new ClaimParty(claim, party, role);
  }

  /**
   * Whether this involvement is the given party in the given role.
   *
   * @param other party
   * @param otherRole role
   * @return true when both match
   */
  boolean matches(Party other, ClaimPartyRole otherRole) {
    return role == otherRole && Objects.equals(party.getId(), other.getId());
  }

  public Claim getClaim() {
    return claim;
  }

  public Party getParty() {
    return party;
  }

  public ClaimPartyRole getRole() {
    return role;
  }
}
