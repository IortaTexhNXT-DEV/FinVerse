package com.iortatechnxt.brokerverse.quotation.api.dto;

import com.iortatechnxt.brokerverse.account.domain.AccountPremium;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationContent;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationItem;
import com.iortatechnxt.brokerverse.quotation.service.QuotationPricing;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The content of a quotation version: terms, items with their premium, and the breakdown.
 *
 * @param insurerCode insurer
 * @param insurerBranch insurer branch
 * @param periodFrom period start
 * @param periodTo period end
 * @param validUntil validity
 * @param directPayment direct payment to the insurer
 * @param ratingBasis rating basis
 * @param remarks remarks
 * @param items items
 * @param premium premium breakdown
 * @param totalSumInsured total sum insured
 * @param groups risk groups
 * @param rated whether the premium is computed
 */
public record QuotationContentResponse(
    String insurerCode,
    String insurerBranch,
    LocalDate periodFrom,
    LocalDate periodTo,
    LocalDate validUntil,
    boolean directPayment,
    String ratingBasis,
    String remarks,
    List<Item> items,
    AccountPremium premium,
    BigDecimal totalSumInsured,
    List<Integer> groups,
    boolean rated) {

  /**
   * One item.
   *
   * @param itemNo position (1..n)
   * @param riskGroup risk group
   * @param label risk label
   * @param sumInsured sum insured
   * @param ratePercent rate applied
   * @param premium premium
   * @param data risk data as entered
   */
  public record Item(
      int itemNo,
      int riskGroup,
      String label,
      BigDecimal sumInsured,
      BigDecimal ratePercent,
      BigDecimal premium,
      RiskItemData data) {}

  /**
   * Maps a content.
   *
   * @param c content
   * @return response
   */
  public static QuotationContentResponse from(QuotationContent c) {
    List<QuotationItem> items = c.items();
    return new QuotationContentResponse(
        c.insurerCode(),
        c.insurerBranch(),
        c.periodFrom(),
        c.periodTo(),
        c.validUntil(),
        c.directPayment(),
        c.ratingBasis(),
        c.remarks(),
        IntStream.range(0, items.size())
            .mapToObj(
                n ->
                    new Item(
                        n + 1,
                        items.get(n).riskGroup(),
                        QuotationPricing.label(items.get(n).data()),
                        QuotationPricing.sumInsured(items.get(n).data()),
                        items.get(n).ratePercent(),
                        items.get(n).premium(),
                        items.get(n).data()))
            .toList(),
        c.premium(),
        QuotationPricing.totalSumInsured(c),
        c.groups(),
        c.isRated());
  }
}
