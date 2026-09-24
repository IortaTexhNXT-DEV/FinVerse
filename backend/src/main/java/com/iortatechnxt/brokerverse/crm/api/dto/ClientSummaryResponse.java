package com.iortatechnxt.brokerverse.crm.api.dto;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;

/**
 * Client summary for pickers and headers.
 *
 * @param id id
 * @param code client code (or prospect code before confirmation)
 * @param prospectCode prospect code
 * @param clientCode client code once confirmed
 * @param displayName name
 * @param clientType type
 * @param status status
 * @param kycStatus KYC status
 * @param email e-mail
 * @param mobile mobile
 * @param marketSegment market segment
 * @param partyCode sub-ledger party
 */
public record ClientSummaryResponse(
    Long id,
    String code,
    String prospectCode,
    String clientCode,
    String displayName,
    ClientType clientType,
    ClientStatus status,
    KycStatus kycStatus,
    String email,
    String mobile,
    String marketSegment,
    String partyCode) {

  /**
   * Maps a client.
   *
   * @param c client
   * @return summary
   */
  public static ClientSummaryResponse from(Client c) {
    return new ClientSummaryResponse(
        c.getId(),
        c.getCode(),
        c.getProspectCode(),
        c.getClientCode(),
        c.getDisplayName(),
        c.getClientType(),
        c.getStatus(),
        c.getKycStatus(),
        c.getEmail(),
        c.getMobile(),
        c.getMarketSegment(),
        c.getPartyCode());
  }
}
