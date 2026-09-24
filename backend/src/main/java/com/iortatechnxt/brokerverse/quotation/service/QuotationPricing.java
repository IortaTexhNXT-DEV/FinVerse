package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.account.domain.AccountPremium;
import com.iortatechnxt.brokerverse.account.domain.InsuredItem;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.TsuRule.TsuFacts;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown.ItemPremium;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery;
import com.iortatechnxt.brokerverse.catalog.service.RatingService;
import com.iortatechnxt.brokerverse.catalog.service.RatingService.Rating;
import com.iortatechnxt.brokerverse.catalog.service.TsuRoutingService;
import com.iortatechnxt.brokerverse.catalog.service.TsuRoutingService.TsuDecision;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationContent;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Prices a quotation with the Appendix A calculator of the catalog ({@code RatingService},
 * BRNB.043) and evaluates the TSU routing rules (BRNB.098, shown to Marketing). Content that is not
 * yet rateable (an item without sum insured, or without rate when the product has no default rate)
 * stays unrated; submission requires a premium.
 */
@Component
public class QuotationPricing {

  private final RatingService rating;
  private final TsuRoutingService tsu;

  /**
   * Creates the pricing.
   *
   * @param rating rating service
   * @param tsu TSU routing rules
   */
  public QuotationPricing(RatingService rating, TsuRoutingService tsu) {
    this.rating = rating;
    this.tsu = tsu;
  }

  /**
   * Prices every item of a content.
   *
   * @param companyId company
   * @param product product
   * @param content content as entered
   * @return content with item premiums and the breakdown (unrated when not rateable)
   */
  public QuotationContent price(Long companyId, RiskProduct product, QuotationContent content) {
    if (!rateable(product, content)) {
      return withPremium(
          content,
          content.items().stream().map(QuotationPricing::unrated).toList(),
          AccountPremium.NONE);
    }
    Rating result = rate(companyId, product, content, content.items());
    PremiumBreakdown b = result.breakdown();
    List<QuotationItem> items = new ArrayList<>();
    for (int i = 0; i < content.items().size(); i++) {
      QuotationItem item = content.items().get(i);
      ItemPremium p = i < b.items().size() ? b.items().get(i) : null;
      items.add(
          new QuotationItem(
              item.riskGroup(),
              item.data(),
              p == null ? null : p.premium(),
              p == null ? null : p.ratePercent()));
    }
    return withPremium(content, items, premiumOf(result, content.ratingBasis()));
  }

  /**
   * The premium breakdown of one risk group (one account), as passed to the account module.
   *
   * @param companyId company
   * @param product product
   * @param content content
   * @param group risk group
   * @return breakdown, null when the group is not rateable
   */
  public PremiumBreakdown rateGroup(
      Long companyId, RiskProduct product, QuotationContent content, int group) {
    List<QuotationItem> items = content.itemsOf(group);
    if (items.isEmpty() || !rateable(product, content)) {
      return null;
    }
    return rate(companyId, product, content, items).breakdown();
  }

  /**
   * Whether the content meets a TSU routing rule (fleet, locations, TSI, non-package).
   *
   * @param product product
   * @param content content
   * @return decision
   */
  public TsuDecision tsu(RiskProduct product, QuotationContent content) {
    List<RiskItemData> data = content.items().stream().map(QuotationItem::data).toList();
    return tsu.evaluate(
        product,
        new TsuFacts(
            product.isPackaged(),
            product.getLineCode(),
            (int) data.stream().filter(d -> d.vehicle() != null).count(),
            (int) data.stream().filter(d -> d.location() != null).count(),
            totalSumInsured(content),
            null));
  }

  /**
   * Total sum insured of a content.
   *
   * @param content content
   * @return total
   */
  public static BigDecimal totalSumInsured(QuotationContent content) {
    return content.items().stream()
        .map(i -> sumInsured(i.data()))
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Sum insured of an item: the total of the items insured at a location when given.
   *
   * @param data item
   * @return sum insured, null when unknown
   */
  public static BigDecimal sumInsured(RiskItemData data) {
    if (data.location() != null && !data.location().insuredItems().isEmpty()) {
      return data.location().insuredItems().stream()
          .map(InsuredItem::sumInsured)
          .filter(Objects::nonNull)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    return data.sumInsured();
  }

  /**
   * Short label of an item: plate, address, person or description.
   *
   * @param data item
   * @return label
   */
  public static String label(RiskItemData data) {
    Stream<String> candidates =
        Stream.of(
            data.vehicle() == null ? null : data.vehicle().plateNo(),
            data.vehicle() == null ? null : data.vehicle().conductionSticker(),
            data.location() == null ? null : data.location().address(),
            data.person() == null ? null : data.person().name(),
            data.description());
    return candidates.filter(s -> s != null && !s.isBlank()).findFirst().orElse("Item");
  }

  private Rating rate(
      Long companyId, RiskProduct product, QuotationContent content, List<QuotationItem> items) {
    return rating.rate(
        new RatingQuery(
            companyId,
            product.getCode(),
            content.insurerCode(),
            content.insurerCode() == null ? null : content.insurerBranch(),
            items.stream()
                .map(
                    i ->
                        new RatingQuery.Item(
                            label(i.data()),
                            sumInsured(i.data()),
                            i.data().rate(),
                            i.data().biLimit(),
                            i.data().pdLimit()))
                .toList(),
            false,
            basisOf(content.ratingBasis()),
            content.periodFrom(),
            content.periodTo(),
            null,
            false,
            null));
  }

  private static boolean rateable(RiskProduct product, QuotationContent content) {
    boolean itemsReady =
        !content.items().isEmpty()
            && content.items().stream()
                .allMatch(
                    i ->
                        sumInsured(i.data()) != null
                            && (i.data().rate() != null || product.getDefaultRate() != null));
    boolean periodReady =
        basisOf(content.ratingBasis()) == PeriodBasis.ANNUAL
            || content.periodFrom() != null
                && content.periodTo() != null
                && content.periodTo().isAfter(content.periodFrom());
    return itemsReady && periodReady;
  }

  private static PeriodBasis basisOf(String basis) {
    return basis == null || basis.isBlank() ? PeriodBasis.ANNUAL : PeriodBasis.valueOf(basis);
  }

  private static AccountPremium premiumOf(Rating result, String basis) {
    PremiumBreakdown b = result.breakdown();
    return new AccountPremium(
        basisOf(basis).name(),
        b.netPremium(),
        b.dst(),
        b.premiumTax(),
        b.vat(),
        b.fst(),
        b.lgt(),
        b.totalCharges(),
        b.grossPremium(),
        result.rates().commission(),
        b.commission(),
        b.vatOnCommission(),
        b.minimumApplied());
  }

  private static QuotationItem unrated(QuotationItem item) {
    return new QuotationItem(item.riskGroup(), item.data(), null, null);
  }

  private static QuotationContent withPremium(
      QuotationContent c, List<QuotationItem> items, AccountPremium premium) {
    return new QuotationContent(
        c.insurerCode(),
        c.insurerBranch(),
        c.periodFrom(),
        c.periodTo(),
        c.validUntil(),
        c.directPayment(),
        c.ratingBasis(),
        c.remarks(),
        items,
        premium);
  }
}
