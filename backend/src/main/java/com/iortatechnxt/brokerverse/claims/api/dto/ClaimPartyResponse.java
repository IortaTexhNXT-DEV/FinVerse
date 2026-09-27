package com.iortatechnxt.brokerverse.claims.api.dto;

import com.iortatechnxt.brokerverse.claims.domain.ClaimParty;
import com.iortatechnxt.brokerverse.claims.domain.ClaimPartyRole;
import com.iortatechnxt.brokerverse.party.domain.PartyType;

/**
 * A party involved in a claim.
 *
 * @param role role
 * @param partyCode party code
 * @param partyName party name
 * @param partyType party type
 */
public record ClaimPartyResponse(
    ClaimPartyRole role, String partyCode, String partyName, PartyType partyType) {

  /**
   * Maps an involved party.
   *
   * @param p involved party (party loaded)
   * @return response
   */
  public static ClaimPartyResponse from(ClaimParty p) {
    return new ClaimPartyResponse(
        p.getRole(), p.getParty().getCode(), p.getParty().getName(), p.getParty().getPartyType());
  }
}
