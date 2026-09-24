package com.iortatechnxt.brokerverse.account.api.dto;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
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
    Instant createdAt) {

  /**
   * Maps an account (scalar columns only).
   *
   * @param a account
   * @return response
   */
  public static AccountSummaryResponse from(Account a) {
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
        a.getCreatedAt());
  }
}
