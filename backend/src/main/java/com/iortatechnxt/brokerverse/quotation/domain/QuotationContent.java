package com.iortatechnxt.brokerverse.quotation.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iortatechnxt.brokerverse.account.domain.AccountPremium;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * The commercial content of one quotation version (BRNB.020): what the client is offered. Stored as
 * JSON per version; the client and product are fixed on the quotation itself.
 *
 * @param insurerCode insurer party code, may be null (insurer chosen at placement)
 * @param insurerBranch insurer branch (LGT)
 * @param periodFrom period start
 * @param periodTo period end
 * @param validUntil last day the offer may be accepted
 * @param directPayment premium paid directly to the insurer (MKTID.011)
 * @param ratingBasis period basis of the premium (ANNUAL, PRO_RATA, SHORT_PERIOD)
 * @param remarks remarks printed on the quotation
 * @param items risk items with their premium
 * @param premium premium breakdown of all items, {@link AccountPremium#NONE} until rated
 * @param schemeVersion package version that priced the content (BRPM.007), null for a product
 *     without versions or content written before versions existed
 * @param schemeDeviation an item rate differs from the scheme rate: submission needs an approved
 *     rate exception
 */
public record QuotationContent(
    String insurerCode,
    String insurerBranch,
    LocalDate periodFrom,
    LocalDate periodTo,
    LocalDate validUntil,
    boolean directPayment,
    String ratingBasis,
    String remarks,
    List<QuotationItem> items,
    AccountPremium premium,
    Integer schemeVersion,
    boolean schemeDeviation) {

  /** Defensive copy; a missing premium means not rated. */
  public QuotationContent {
    items = items == null ? List.of() : List.copyOf(items);
    premium = premium == null ? AccountPremium.NONE : premium;
  }

  /**
   * Content not yet priced on a package version.
   *
   * @param insurerCode insurer party code
   * @param insurerBranch insurer branch
   * @param periodFrom period start
   * @param periodTo period end
   * @param validUntil validity
   * @param directPayment direct payment
   * @param ratingBasis rating basis
   * @param remarks remarks
   * @param items items
   * @param premium premium breakdown
   */
  public QuotationContent(
      String insurerCode,
      String insurerBranch,
      LocalDate periodFrom,
      LocalDate periodTo,
      LocalDate validUntil,
      boolean directPayment,
      String ratingBasis,
      String remarks,
      List<QuotationItem> items,
      AccountPremium premium) {
    this(
        insurerCode,
        insurerBranch,
        periodFrom,
        periodTo,
        validUntil,
        directPayment,
        ratingBasis,
        remarks,
        items,
        premium,
        null,
        false);
  }

  /**
   * Risk groups used by the items, ascending.
   *
   * @return groups
   */
  @JsonIgnore
  public List<Integer> groups() {
    return items.stream().map(QuotationItem::riskGroup).distinct().sorted().toList();
  }

  /**
   * The items of one risk group.
   *
   * @param group risk group
   * @return items
   */
  public List<QuotationItem> itemsOf(int group) {
    return items.stream().filter(i -> i.riskGroup() == group).toList();
  }

  /**
   * Whether the whole content is rated.
   *
   * @return true when the premium is computed
   */
  @JsonIgnore
  public boolean isRated() {
    return premium.isRated() && items.stream().allMatch(i -> Objects.nonNull(i.premium()));
  }
}
