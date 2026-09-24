package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountPremium;
import com.iortatechnxt.brokerverse.account.domain.RiskItem;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown.ItemPremium;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery;
import com.iortatechnxt.brokerverse.catalog.service.RatingService;
import com.iortatechnxt.brokerverse.catalog.service.RatingService.Rating;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Prices an account with the Appendix A calculator (BRNB.051 "auto calculations": pro-rata,
 * short-term, period) and stores the breakdown on the account and its items. A draft that is not
 * yet rateable (an item without sum insured, or without rate when the product has no default) is
 * saved unrated; submission requires a premium.
 */
@Component
public class AccountPricing {

  private static final int RATE_SCALE = 8;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final RatingService rating;

  /**
   * Creates the pricing.
   *
   * @param rating rating service
   */
  public AccountPricing(RatingService rating) {
    this.rating = rating;
  }

  /**
   * Prices an account.
   *
   * @param account account (items applied)
   * @param product product
   * @param terms period basis and commission override
   * @param given premium computed elsewhere (quotation), null to rate now
   */
  public void price(Account account, RiskProduct product, Terms terms, PremiumBreakdown given) {
    PeriodBasis basis = terms.basis() == null ? PeriodBasis.ANNUAL : terms.basis();
    if (given != null) {
      store(account, given, commissionRateOf(given), PeriodBasis.ANNUAL);
    } else if (rateable(account, product, basis)) {
      rate(account, product, terms, basis);
    } else {
      account.setPremium(AccountPremium.NONE);
      account.getItems().forEach(i -> i.rated(null, null));
    }
  }

  private void rate(Account account, RiskProduct product, Terms terms, PeriodBasis basis) {
    Rating result =
        rating.rate(
            new RatingQuery(
                account.getCompanyId(),
                product.getCode(),
                account.getInsurerCode(),
                account.getInsurerBranch(),
                account.getItems().stream()
                    .map(
                        i ->
                            new RatingQuery.Item(
                                i.label(),
                                i.getSumInsured(),
                                i.getRate(),
                                i.getBiLimit(),
                                i.getPdLimit()))
                    .toList(),
                account.isMultiYear(),
                basis,
                account.getPeriodFrom(),
                account.getPeriodTo(),
                terms.commissionRate(),
                false,
                null));
    store(account, result.breakdown(), result.rates().commission(), basis);
  }

  private static boolean rateable(Account account, RiskProduct product, PeriodBasis basis) {
    List<RiskItem> items = account.getItems();
    boolean itemsReady =
        !items.isEmpty()
            && items.stream()
                .allMatch(
                    i ->
                        i.getSumInsured() != null
                            && (i.getRate() != null || product.getDefaultRate() != null));
    boolean periodReady =
        basis == PeriodBasis.ANNUAL
            || account.getPeriodFrom() != null
                && account.getPeriodTo() != null
                && account.getPeriodTo().isAfter(account.getPeriodFrom());
    return itemsReady && periodReady;
  }

  private static void store(
      Account account, PremiumBreakdown b, BigDecimal commissionRate, PeriodBasis basis) {
    account.setPremium(
        new AccountPremium(
            basis.name(),
            b.netPremium(),
            b.dst(),
            b.premiumTax(),
            b.vat(),
            b.fst(),
            b.lgt(),
            b.totalCharges(),
            b.grossPremium(),
            commissionRate,
            b.commission(),
            b.vatOnCommission(),
            b.minimumApplied()));
    List<RiskItem> items = account.getItems();
    List<ItemPremium> priced = b.items();
    for (int i = 0; i < items.size(); i++) {
      ItemPremium p = i < priced.size() ? priced.get(i) : null;
      items.get(i).rated(p == null ? null : p.premium(), p == null ? null : p.ratePercent());
    }
  }

  private static BigDecimal commissionRateOf(PremiumBreakdown b) {
    if (b.netPremium() == null || b.netPremium().signum() == 0 || b.commission() == null) {
      return null;
    }
    return b.commission()
        .multiply(HUNDRED)
        .divide(b.netPremium(), RATE_SCALE, RoundingMode.HALF_UP);
  }

  /**
   * Pricing terms chosen on the account.
   *
   * @param basis period basis
   * @param commissionRate commission override in percent
   */
  public record Terms(PeriodBasis basis, BigDecimal commissionRate) {}
}
