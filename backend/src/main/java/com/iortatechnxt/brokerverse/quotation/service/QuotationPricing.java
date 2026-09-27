package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.account.domain.AccountPremium;
import com.iortatechnxt.brokerverse.account.domain.InsuredItem;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.TsuRule.TsuFacts;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown.ItemPremium;
import com.iortatechnxt.brokerverse.catalog.service.RateSchemeExceptionService;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery;
import com.iortatechnxt.brokerverse.catalog.service.RatingService;
import com.iortatechnxt.brokerverse.catalog.service.RatingService.Rating;
import com.iortatechnxt.brokerverse.catalog.service.TsuRoutingService;
import com.iortatechnxt.brokerverse.catalog.service.TsuRoutingService.TsuDecision;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
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
 *
 * <p>Quotations are new business (BRPM.007): a package is priced on its current released version,
 * which the content records. A draft may carry an item rate other than the scheme rate (flagged);
 * submission and account creation need the approved rate exception of the quotation for it.
 */
@Component
public class QuotationPricing {

  private final RatingService rating;
  private final TsuRoutingService tsu;
  private final RateSchemeExceptionService exceptions;

  /**
   * Creates the pricing.
   *
   * @param rating rating service
   * @param tsu TSU routing rules
   * @param exceptions approved rate-scheme exceptions of a quotation
   */
  public QuotationPricing(
      RatingService rating, TsuRoutingService tsu, RateSchemeExceptionService exceptions) {
    this.rating = rating;
    this.tsu = tsu;
    this.exceptions = exceptions;
  }

  /**
   * Prices every item of a content (wizard preview: no quotation number, so no exception).
   *
   * @param companyId company
   * @param product product
   * @param content content as entered
   * @return content with item premiums and the breakdown (unrated when not rateable)
   */
  public QuotationContent price(Long companyId, RiskProduct product, QuotationContent content) {
    return price(companyId, product, content, null);
  }

  /**
   * Prices every item of a quotation's content on the current package version, with the quotation's
   * approved rate exception when it has one.
   *
   * @param companyId company
   * @param product product
   * @param content content as entered
   * @param quotationNo quotation number, null before creation
   * @return content with item premiums, breakdown and version (unrated when not rateable)
   */
  public QuotationContent price(
      Long companyId, RiskProduct product, QuotationContent content, String quotationNo) {
    if (!rateable(product, content)) {
      return withPremium(
          content,
          content.items().stream().map(QuotationPricing::unrated).toList(),
          AccountPremium.NONE,
          null);
    }
    Rating result =
        rate(
            companyId,
            product,
            content,
            content.items(),
            new Scheme(null, overrideOf(quotationNo, product), false));
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
    return withPremium(content, items, premiumOf(result, content.ratingBasis()), result);
  }

  /**
   * Checks the submitted content against the package rate scheme (BRPM.007): it must be priced on
   * the current version, and an item rate other than the scheme rate needs the approved exception
   * of the quotation ({@code RATE_SCHEME_NOT_CURRENT}).
   *
   * @param companyId company
   * @param product product
   * @param content content (rated)
   * @param quotationNo quotation number
   * @return the exception reference used, null when none was needed
   */
  public String requireScheme(
      Long companyId, RiskProduct product, QuotationContent content, String quotationNo) {
    Rating strict =
        rate(
            companyId,
            product,
            content,
            content.items(),
            new Scheme(null, overrideOf(quotationNo, product), true));
    if (content.schemeVersion() != null
        && !content.schemeVersion().equals(strict.schemeVersion())) {
      throw new BusinessRuleException(
          RateSchemeExceptionService.NOT_CURRENT,
          "The quotation is priced on version "
              + content.schemeVersion()
              + " of "
              + product.getCode()
              + "; version "
              + strict.schemeVersion()
              + " is current: save the quotation again to re-price it");
    }
    return strict.overrideRef();
  }

  /**
   * The premium breakdown of one risk group (one account), as passed to the account module: new
   * business on the version the quotation was priced on, which must still be current unless the
   * quotation's approved exception allows it (BRPM.007).
   *
   * @param companyId company
   * @param product product
   * @param content content
   * @param group risk group
   * @param overrideRef approved exception of the quotation, null when none
   * @return breakdown, null when the group is not rateable
   */
  public PremiumBreakdown rateGroup(
      Long companyId,
      RiskProduct product,
      QuotationContent content,
      int group,
      String overrideRef) {
    List<QuotationItem> items = content.itemsOf(group);
    if (items.isEmpty() || !rateable(product, content)) {
      return null;
    }
    return rate(
            companyId,
            product,
            content,
            items,
            new Scheme(content.schemeVersion(), overrideRef, true))
        .breakdown();
  }

  private String overrideOf(String quotationNo, RiskProduct product) {
    return exceptions.latestApproved(quotationNo, product.getCode());
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
      Long companyId,
      RiskProduct product,
      QuotationContent content,
      List<QuotationItem> items,
      Scheme scheme) {
    RatingQuery query =
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
            null,
            RatingQuery.Purpose.NEW_BUSINESS,
            scheme.version(),
            scheme.overrideRef());
    return scheme.strict() ? rating.rate(query) : rating.rateDraft(query);
  }

  /**
   * How a quotation is priced.
   *
   * @param version package version wanted, null for the current one
   * @param overrideRef approved exception, null when none
   * @param strict refuse item rates other than the scheme rate without the exception
   */
  private record Scheme(Integer version, String overrideRef, boolean strict) {}

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
      QuotationContent c, List<QuotationItem> items, AccountPremium premium, Rating rated) {
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
        premium,
        rated == null ? null : rated.schemeVersion(),
        rated != null && rated.schemeDeviation());
  }
}
