package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import java.time.LocalDate;
import java.util.List;

/**
 * A quotation as entered on the wizard, a bulk row or a request.
 *
 * @param clientId crm client id (a prospect is allowed, BRNB.063)
 * @param productCode risk code
 * @param marketSegment market segment (list MARKET_SEGMENT)
 * @param sourceChannel source channel (list SOURCE_CHANNEL)
 * @param requestId quotation request answered, may be null
 * @param currency currency; PHP when empty
 * @param terms insurer, period, validity, direct payment and remarks
 * @param items risk items with their risk group
 */
public record QuotationDraft(
    Long clientId,
    String productCode,
    String marketSegment,
    String sourceChannel,
    Long requestId,
    String currency,
    Terms terms,
    List<DraftItem> items) {

  /** Defensive copy. */
  public QuotationDraft {
    items = items == null ? List.of() : List.copyOf(items);
  }

  /**
   * The commercial terms of the offer.
   *
   * @param insurerCode insurer party code, may be null
   * @param insurerBranch insurer branch (LGT)
   * @param periodFrom period start
   * @param periodTo period end
   * @param validUntil validity; null for the default validity
   * @param directPayment premium paid directly to the insurer (MKTID.011)
   * @param ratingBasis ANNUAL, PRO_RATA or SHORT_PERIOD; null for annual
   * @param remarks remarks
   */
  public record Terms(
      String insurerCode,
      String insurerBranch,
      LocalDate periodFrom,
      LocalDate periodTo,
      LocalDate validUntil,
      boolean directPayment,
      String ratingBasis,
      String remarks) {}

  /**
   * An item and the risk group (account) it belongs to.
   *
   * @param riskGroup risk group, 1 when empty
   * @param data risk data
   */
  public record DraftItem(Integer riskGroup, RiskItemData data) {

    /**
     * The risk group, 1 by default.
     *
     * @return group
     */
    public int group() {
      return riskGroup == null || riskGroup < 1 ? 1 : riskGroup;
    }
  }
}
