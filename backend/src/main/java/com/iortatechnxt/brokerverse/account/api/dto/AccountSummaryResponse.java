package com.iortatechnxt.brokerverse.account.api.dto;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountLegacyHeader;
import com.iortatechnxt.brokerverse.account.domain.AccountOrigin;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * An account in lists and search results.
 *
 * @param id id
 * @param arn Account Reference Number
 * @param clientCode client code
 * @param clientName client name
 * @param productCode product
 * @param lineCode product line
 * @param insurerCode insurer
 * @param status status
 * @param periodFrom period start
 * @param periodTo period end
 * @param totalSumInsured total sum insured
 * @param grossPremium gross premium
 * @param currency currency
 * @param ffy Free First Year tagged
 * @param ffyStart FFY start
 * @param ffyEnd FFY end
 * @param paymentArrangement payment arrangement
 * @param directPayment direct payment flag
 * @param accountOfficer account officer
 * @param createdAt creation time
 * @param businessType New Business or Renewal (BRNB.097, BT0)
 * @param origin how the account was created (MIGRATED: imported from a legacy system)
 * @param sourceSystem legacy source system of an imported account
 * @param legacyRef legacy policy reference
 * @param migrationBatch migration batch that imported it
 */
public record AccountSummaryResponse(
    Long id,
    String arn,
    String clientCode,
    String clientName,
    String productCode,
    String lineCode,
    String insurerCode,
    AccountStatus status,
    LocalDate periodFrom,
    LocalDate periodTo,
    BigDecimal totalSumInsured,
    BigDecimal grossPremium,
    String currency,
    boolean ffy,
    LocalDate ffyStart,
    LocalDate ffyEnd,
    PaymentArrangement paymentArrangement,
    boolean directPayment,
    String accountOfficer,
    Instant createdAt,
    BusinessType businessType,
    AccountOrigin origin,
    String sourceSystem,
    String legacyRef,
    String migrationBatch) {

  /**
   * Maps an account (scalar columns only).
   *
   * @param a account
   * @return response
   */
  public static AccountSummaryResponse from(Account a) {
    return from(a, null);
  }

  /**
   * Maps an account with the legacy header of an imported account.
   *
   * @param a account
   * @param legacy legacy header, null for an account created in BIBS
   * @return response
   */
  public static AccountSummaryResponse from(Account a, AccountLegacyHeader legacy) {
    return new AccountSummaryResponse(
        a.getId(),
        a.getArn(),
        a.getClientCode(),
        a.getClientName(),
        a.getProductCode(),
        a.getLineCode(),
        a.getInsurerCode(),
        a.getStatus(),
        a.getPeriodFrom(),
        a.getPeriodTo(),
        a.getTotalSumInsured(),
        a.getPremium().grossPremium(),
        a.getCurrency(),
        a.getFreeFirstYear().active(),
        a.getFreeFirstYear().start(),
        a.getFreeFirstYear().end(),
        a.getPaymentArrangement(),
        a.isDirectPayment(),
        a.getSales().accountOfficer(),
        a.getCreatedAt(),
        a.getBusinessType(),
        a.getClassification().origin(),
        legacy == null ? null : legacy.getSourceSystem(),
        legacy == null ? null : legacy.getLegacyRef(),
        legacy == null ? null : legacy.getMigrationBatch());
  }
}
