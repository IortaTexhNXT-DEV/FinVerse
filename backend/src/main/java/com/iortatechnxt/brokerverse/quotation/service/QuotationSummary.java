package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A quotation as seen by other modules (contract of {@link QuotationQueryService#getByArn}, used by
 * Operations Cashiering): references, client, product, premium and the direct-payment flag
 * (MKTID.011).
 *
 * @param id quotation id
 * @param quotationNo quotation number (Proposal No.)
 * @param arn Account Reference Number
 * @param clientId crm client id
 * @param clientCode client or prospect code
 * @param clientName client name
 * @param productCode product
 * @param insurerCode insurer, may be null
 * @param status status
 * @param currency currency
 * @param grossPremium gross premium of the current version, null when not rated
 * @param validUntil validity
 * @param directPayment premium paid directly to the insurer
 * @param accountArns accounts created from the quotation
 * @param productVersionNo package version that priced the current version (BRPM.007), null when
 *     none
 */
public record QuotationSummary(
    Long id,
    String quotationNo,
    String arn,
    Long clientId,
    String clientCode,
    String clientName,
    String productCode,
    String insurerCode,
    QuotationStatus status,
    String currency,
    BigDecimal grossPremium,
    LocalDate validUntil,
    boolean directPayment,
    List<String> accountArns,
    Integer productVersionNo) {

  /** Defensive copy. */
  public QuotationSummary {
    accountArns = List.copyOf(accountArns);
  }

  /**
   * Summary of a quotation.
   *
   * @param q quotation
   * @return summary
   */
  public static QuotationSummary of(Quotation q) {
    return new QuotationSummary(
        q.getId(),
        q.getQuotationNo(),
        q.getArn(),
        q.getClientId(),
        q.getClientCode(),
        q.getClientName(),
        q.getProductCode(),
        q.getInsurerCode(),
        q.getStatus(),
        q.getCurrency(),
        q.getGrossPremium(),
        q.getValidUntil(),
        q.isDirectPayment(),
        q.getAccountArns(),
        q.getProductVersionNo());
  }
}
