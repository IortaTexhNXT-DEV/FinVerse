package com.iortatechnxt.brokerverse.quotation.api.dto;

import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A row of the quotation list.
 *
 * @param id id
 * @param quotationNo quotation number (Proposal No.)
 * @param arn Account Reference Number
 * @param clientId client
 * @param clientCode client or prospect code
 * @param clientName client name
 * @param productCode product
 * @param insurerCode insurer
 * @param totalSumInsured total sum insured
 * @param grossPremium gross premium
 * @param currency currency
 * @param validUntil validity
 * @param directPayment direct payment to the insurer
 * @param riskGroups number of risk groups (accounts)
 * @param currentVersion current version
 * @param status status
 * @param createdBy maker
 * @param createdAt created
 */
public record QuotationListItem(
    Long id,
    String quotationNo,
    String arn,
    Long clientId,
    String clientCode,
    String clientName,
    String productCode,
    String insurerCode,
    BigDecimal totalSumInsured,
    BigDecimal grossPremium,
    String currency,
    LocalDate validUntil,
    boolean directPayment,
    int riskGroups,
    int currentVersion,
    QuotationStatus status,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps a quotation.
   *
   * @param q quotation
   * @return row
   */
  public static QuotationListItem from(Quotation q) {
    return new QuotationListItem(
        q.getId(),
        q.getQuotationNo(),
        q.getArn(),
        q.getClientId(),
        q.getClientCode(),
        q.getClientName(),
        q.getProductCode(),
        q.getInsurerCode(),
        q.getTotalSumInsured(),
        q.getGrossPremium(),
        q.getCurrency(),
        q.getValidUntil(),
        q.isDirectPayment(),
        q.getRiskGroups(),
        q.getCurrentVersion(),
        q.getStatus(),
        q.getCreatedBy(),
        q.getCreatedAt());
  }
}
