package com.iortatechnxt.brokerverse.renewal.extraction.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A booked root invoice expiring in the extraction range, with the facts of its account.
 *
 * @param invoiceNo invoice number
 * @param arn ARN
 * @param policyYear policy year
 * @param facts policy, PN, insurer, currency and period
 * @param client client
 * @param product product and classification
 * @param sales sales organisation
 * @param amounts premium, sum insured, commission rate and mortgagee
 * @param lastYear whether the invoice ends the account's term (multi-year accounts)
 * @param extracted whether a candidate already exists for it
 */
public record ExpiringInvoice(
    String invoiceNo,
    String arn,
    Integer policyYear,
    Facts facts,
    Client client,
    Product product,
    Sales sales,
    Amounts amounts,
    boolean lastYear,
    boolean extracted) {

  /**
   * Policy facts.
   *
   * @param policyNo policy number
   * @param pnNos PN numbers
   * @param insurerCode lead insurer
   * @param currency currency
   * @param inception period start
   * @param expiry period end
   */
  public record Facts(
      String policyNo,
      String pnNos,
      String insurerCode,
      String currency,
      LocalDate inception,
      LocalDate expiry) {}

  /**
   * Client of the account.
   *
   * @param id crm id
   * @param code client code
   * @param name client name
   * @param assured assured name
   * @param email e-mail of the account contact or the client
   * @param type client type (account type)
   */
  public record Client(
      Long id, String code, String name, String assured, String email, String type) {}

  /**
   * Product of the account.
   *
   * @param code risk code
   * @param name risk name
   * @param line product line
   * @param segment market segment
   * @param origin business origin (source channel)
   * @param packaged packaged product
   * @param versionNo package version
   */
  public record Product(
      String code,
      String name,
      String line,
      String segment,
      String origin,
      boolean packaged,
      Integer versionNo) {}

  /**
   * Sales organisation.
   *
   * @param branch invoicing branch
   * @param region region
   * @param department department
   * @param team sales team
   * @param officer account officer
   */
  public record Sales(
      String branch, String region, String department, String team, String officer) {}

  /**
   * Amounts.
   *
   * @param netPremium basic premium
   * @param grossPremium gross premium
   * @param sumInsured total sum insured
   * @param commissionRate commission rate
   * @param mortgagee mortgagee bank
   */
  public record Amounts(
      BigDecimal netPremium,
      BigDecimal grossPremium,
      BigDecimal sumInsured,
      BigDecimal commissionRate,
      String mortgagee) {}
}
