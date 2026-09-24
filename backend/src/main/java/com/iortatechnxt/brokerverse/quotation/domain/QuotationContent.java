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
    AccountPremium premium) {

  /** Defensive copy; a missing premium means not rated. */
  public QuotationContent {
    items = items == null ? List.of() : List.copyOf(items);
    premium = premium == null ? AccountPremium.NONE : premium;
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
