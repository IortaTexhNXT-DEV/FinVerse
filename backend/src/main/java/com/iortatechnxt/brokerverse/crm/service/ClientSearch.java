package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.common.domain.RecordOrigin;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;

/**
 * Client search criteria (BRNB.046); null criteria are ignored.
 *
 * @param companyId company
 * @param code prospect or client code (prefix)
 * @param name name contains
 * @param tin TIN
 * @param idNumber ID number
 * @param email e-mail
 * @param mobile mobile number (any format)
 * @param status status
 * @param kycStatus KYC status
 * @param marketSegment market segment
 * @param bankClient BDO bank client flag
 * @param clientType client type
 * @param kycDue only clients whose KYC is expired or comes due within the review window
 * @param origin BIBS or migrated clients, null for both
 */
public record ClientSearch(
    Long companyId,
    String code,
    String name,
    String tin,
    String idNumber,
    String email,
    String mobile,
    ClientStatus status,
    KycStatus kycStatus,
    String marketSegment,
    Boolean bankClient,
    ClientType clientType,
    boolean kycDue,
    RecordOrigin.Origin origin) {

  /**
   * Criteria without the origin filter.
   *
   * @param companyId company
   * @param code prospect or client code (prefix)
   * @param name name contains
   * @param tin TIN
   * @param idNumber ID number
   * @param email e-mail
   * @param mobile mobile number
   * @param status status
   * @param kycStatus KYC status
   * @param marketSegment market segment
   * @param bankClient BDO bank client flag
   * @param clientType client type
   * @param kycDue only clients whose KYC is due
   */
  @SuppressWarnings("java:S107") // search criteria
  public ClientSearch(
      Long companyId,
      String code,
      String name,
      String tin,
      String idNumber,
      String email,
      String mobile,
      ClientStatus status,
      KycStatus kycStatus,
      String marketSegment,
      Boolean bankClient,
      ClientType clientType,
      boolean kycDue) {
    this(
        companyId,
        code,
        name,
        tin,
        idNumber,
        email,
        mobile,
        status,
        kycStatus,
        marketSegment,
        bankClient,
        clientType,
        kycDue,
        null);
  }
}
