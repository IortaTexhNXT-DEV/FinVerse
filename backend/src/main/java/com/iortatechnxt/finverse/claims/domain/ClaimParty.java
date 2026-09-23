package com.iortatechnxt.finverse.claims.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.party.domain.Party;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

  ClaimParty(Claim claim, Party party, ClaimPartyRole role) {
    this.claim = claim;
    this.party = party;
    this.role = role;
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
