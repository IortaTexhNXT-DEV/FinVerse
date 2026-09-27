package com.iortatechnxt.brokerverse.crm.api.dto;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientProfile;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Complete client details (BRNB.046 "view full details").
 *
 * @param id id
 * @param companyId company
 * @param code client code, or prospect code before confirmation
 * @param prospectCode prospect code
 * @param clientCode client code
 * @param status status
 * @param onboardingStage onboarding workflow stage
 * @param clientType type
 * @param displayName name
 * @param lastName last name
 * @param firstName first name
 * @param middleName middle name
 * @param suffix suffix
 * @param corporateName corporate name
 * @param birthDate birth date
 * @param tin TIN
 * @param idType ID type
 * @param idNumber ID number
 * @param email e-mail
 * @param mobile mobile
 * @param phone landline
 * @param addressLine street address
 * @param city city
 * @param province province
 * @param postalCode postal code
 * @param marketSegment market segment
 * @param bankClient BDO bank client
 * @param bankCif BDO CIF
 * @param profile KYC profile
 * @param partyCode sub-ledger party
 * @param kyc KYC status and dates
 * @param infoComplete whether the configured minimum fields are present (BRNB.029)
 * @param missingFields missing minimum fields
 * @param lifecycle confirmation and deactivation
 */
public record ClientResponse(
    Long id,
    Long companyId,
    String code,
    String prospectCode,
    String clientCode,
    ClientStatus status,
    String onboardingStage,
    ClientType clientType,
    String displayName,
    String lastName,
    String firstName,
    String middleName,
    String suffix,
    String corporateName,
    LocalDate birthDate,
    String tin,
    String idType,
    String idNumber,
    String email,
    String mobile,
    String phone,
    String addressLine,
    String city,
    String province,
    String postalCode,
    String marketSegment,
    boolean bankClient,
    String bankCif,
    ClientProfile profile,
    String partyCode,
    Kyc kyc,
    boolean infoComplete,
    List<String> missingFields,
    Lifecycle lifecycle) {

  /**
   * Maps a client.
   *
   * @param c client
   * @param missing missing minimum fields
   * @return response
   */
  public static ClientResponse from(Client c, List<String> missing) {
    return new ClientResponse(
        c.getId(),
        c.getCompanyId(),
        c.getCode(),
        c.getProspectCode(),
        c.getClientCode(),
        c.getStatus(),
        c.getOnboardingStage(),
        c.getClientType(),
        c.getDisplayName(),
        c.getLastName(),
        c.getFirstName(),
        c.getMiddleName(),
        c.getSuffix(),
        c.getCorporateName(),
        c.getBirthDate(),
        c.getTin(),
        c.getIdType(),
        c.getIdNumber(),
        c.getEmail(),
        c.getMobile(),
        c.getPhone(),
        c.getAddressLine(),
        c.getCity(),
        c.getProvince(),
        c.getPostalCode(),
        c.getMarketSegment(),
        c.isBankClient(),
        c.getBankCif(),
        c.profile(),
        c.getPartyCode(),
        new Kyc(
            c.getKycStatus(),
            c.getKycSubmittedBy(),
            c.getKycSubmittedAt(),
            c.getKycVerifiedBy(),
            c.getKycVerifiedAt(),
            c.getKycReviewDue()),
        missing.isEmpty(),
        List.copyOf(missing),
        new Lifecycle(
            c.getCreatedBy(),
            c.getCreatedAt(),
            c.getConfirmedBy(),
            c.getConfirmedAt(),
            c.getDeactivationReason(),
            c.getDeactivationNote(),
            c.getDeactivatedBy(),
            c.getDeactivatedAt()));
  }

  /**
   * KYC status and dates.
   *
   * @param status KYC status
   * @param submittedBy maker who submitted the KYC
   * @param submittedAt submission time
   * @param verifiedBy checker
   * @param verifiedAt verification time
   * @param reviewDue next periodic review
   */
  public record Kyc(
      KycStatus status,
      String submittedBy,
      Instant submittedAt,
      String verifiedBy,
      Instant verifiedAt,
      LocalDate reviewDue) {}

  /**
   * Who created, confirmed and deactivated the client, and when.
   *
   * @param createdBy maker
   * @param createdAt creation time
   * @param confirmedBy confirming user
   * @param confirmedAt confirmation time
   * @param deactivationReason deactivation reason code
   * @param deactivationNote deactivation comment
   * @param deactivatedBy deactivating user
   * @param deactivatedAt deactivation time
   */
  public record Lifecycle(
      String createdBy,
      Instant createdAt,
      String confirmedBy,
      Instant confirmedAt,
      String deactivationReason,
      String deactivationNote,
      String deactivatedBy,
      Instant deactivatedAt) {}
}
