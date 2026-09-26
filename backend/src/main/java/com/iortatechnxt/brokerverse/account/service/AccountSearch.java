package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import java.time.LocalDate;
import java.util.List;

/**
 * Account search criteria (BRNB.050); null criteria are not applied. Voided accounts are excluded
 * unless asked for (BRNB.019).
 *
 * @param companyId company
 * @param text ARN, client code or client name fragment
 * @param pnNumber promissory note number
 * @param vehicleId plate, conduction sticker, engine or chassis number
 * @param location location of risk text
 * @param productCode product
 * @param lineCode product line
 * @param insurerCode insurer
 * @param statuses statuses
 * @param ffy Free First Year tagged
 * @param directPayment paid directly to the insurer
 * @param periodFrom period start on or after
 * @param periodTo period start on or before
 * @param accountOfficer account officer
 * @param includeVoided include voided accounts
 * @param businessType New Business or Renewal (BRNB.097, BT0), null for both
 */
public record AccountSearch(
    Long companyId,
    String text,
    String pnNumber,
    String vehicleId,
    String location,
    String productCode,
    String lineCode,
    String insurerCode,
    List<AccountStatus> statuses,
    Boolean ffy,
    Boolean directPayment,
    LocalDate periodFrom,
    LocalDate periodTo,
    String accountOfficer,
    boolean includeVoided,
    BusinessType businessType) {

  /** Defensive copy. */
  public AccountSearch {
    statuses = statuses == null ? List.of() : List.copyOf(statuses);
  }

  /**
   * Criteria without the business type filter (both types).
   *
   * @param companyId company
   * @param text ARN, client code or client name fragment
   * @param pnNumber promissory note number
   * @param vehicleId plate, conduction sticker, engine or chassis number
   * @param location location of risk text
   * @param productCode product
   * @param lineCode product line
   * @param insurerCode insurer
   * @param statuses statuses
   * @param ffy Free First Year tagged
   * @param directPayment paid directly to the insurer
   * @param periodFrom period start on or after
   * @param periodTo period start on or before
   * @param accountOfficer account officer
   * @param includeVoided include voided accounts
   */
  public AccountSearch(
      Long companyId,
      String text,
      String pnNumber,
      String vehicleId,
      String location,
      String productCode,
      String lineCode,
      String insurerCode,
      List<AccountStatus> statuses,
      Boolean ffy,
      Boolean directPayment,
      LocalDate periodFrom,
      LocalDate periodTo,
      String accountOfficer,
      boolean includeVoided) {
    this(
        companyId,
        text,
        pnNumber,
        vehicleId,
        location,
        productCode,
        lineCode,
        insurerCode,
        statuses,
        ffy,
        directPayment,
        periodFrom,
        periodTo,
        accountOfficer,
        includeVoided,
        null);
  }

  /**
   * Every account of a company except voided ones.
   *
   * @param companyId company
   * @return criteria
   */
  public static AccountSearch all(Long companyId) {
    return new AccountSearch(
        companyId, null, null, null, null, null, null, null, null, null, null, null, null, null,
        false);
  }
}
