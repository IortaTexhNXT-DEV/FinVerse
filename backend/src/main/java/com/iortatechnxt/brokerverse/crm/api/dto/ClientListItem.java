package com.iortatechnxt.brokerverse.crm.api.dto;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A client in a search result or in the KYC reviews due list.
 *
 * @param id id
 * @param code client or prospect code
 * @param prospectCode prospect code
 * @param clientCode client code
 * @param displayName name
 * @param clientType type
 * @param status status
 * @param onboardingStage onboarding stage
 * @param kycStatus KYC status
 * @param kycReviewDue next KYC review
 * @param kycVerifiedAt last KYC verification
 * @param riskRating risk rating
 * @param tin TIN
 * @param email e-mail
 * @param mobile mobile
 * @param marketSegment market segment
 * @param bankClient BDO bank client
 */
public record ClientListItem(
    Long id,
    String code,
    String prospectCode,
    String clientCode,
    String displayName,
    ClientType clientType,
    ClientStatus status,
    String onboardingStage,
    KycStatus kycStatus,
    LocalDate kycReviewDue,
    Instant kycVerifiedAt,
    String riskRating,
    String tin,
    String email,
    String mobile,
    String marketSegment,
    boolean bankClient) {

  /**
   * Maps a client.
   *
   * @param c client
   * @return item
   */
  public static ClientListItem from(Client c) {
    return new ClientListItem(
        c.getId(),
        c.getCode(),
        c.getProspectCode(),
        c.getClientCode(),
        c.getDisplayName(),
        c.getClientType(),
        c.getStatus(),
        c.getOnboardingStage(),
        c.getKycStatus(),
        c.getKycReviewDue(),
        c.getKycVerifiedAt(),
        c.getRiskRating(),
        c.getTin(),
        c.getEmail(),
        c.getMobile(),
        c.getMarketSegment(),
        c.isBankClient());
  }
}
