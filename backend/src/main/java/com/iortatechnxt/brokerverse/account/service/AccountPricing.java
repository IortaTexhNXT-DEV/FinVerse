package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountPremium;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.account.domain.RiskItem;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown.ItemPremium;
import com.iortatechnxt.brokerverse.catalog.service.RateSchemeExceptionService;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery.Purpose;
import com.iortatechnxt.brokerverse.catalog.service.RatingService;
import com.iortatechnxt.brokerverse.catalog.service.RatingService.Rating;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Prices an account with the Appendix A calculator (BRNB.051 "auto calculations": pro-rata,
 * short-term, period) and stores the breakdown on the account and its items. A draft that is not
 * yet rateable (an item without sum insured, or without rate when the product has no default) is
 * saved unrated; submission requires a premium.
 *
 * <p>A new-business account (BRPM.007) is rated on the package's current released version (or the
 * version an approved rate exception of the account allows), which the account records; a premium
 * computed by the quotation keeps the quotation's version. A RENEWAL account (shared work item BT0)
 * is rated with the RENEWAL purpose on the version it keeps when that version is still released or
 * superseded, else on the current one (PQ11). An item rate other than the scheme rate is refused on
 * submission without the approved exception of the account ({@link #requireScheme}).
 */
@Component
public class AccountPricing {

  private static final int RATE_SCALE = 8;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final RatingService rating;
  private final RateSchemeExceptionService exceptions;

  /**
   * Creates the pricing.
   *
   * @param rating rating service
   * @param exceptions approved rate-scheme exceptions of an account
   */
  public AccountPricing(RatingService rating, RateSchemeExceptionService exceptions) {
    this.rating = rating;
    this.exceptions = exceptions;
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
      Rating result = rating.rateDraft(query(account, product, terms, basis));
      store(account, result.breakdown(), result.rates().commission(), basis);
      account.stampScheme(result.schemeVersion(), result.overrideRef());
    } else {
      account.setPremium(AccountPremium.NONE);
      account.getItems().forEach(i -> i.rated(null, null));
    }
  }

  /**
   * Checks a rated account against the package rate scheme on submission (BRPM.007): the premium
   * must be on the current version, and an item rate other than the scheme rate needs the approved
   * exception of the account (or of its quotation), else {@code RATE_SCHEME_NOT_CURRENT}.
   *
   * @param account account
   * @param product product
   * @param terms rating terms of the account
   */
  public void requireScheme(Account account, RiskProduct product, Terms terms) {
    PeriodBasis basis = terms.basis() == null ? PeriodBasis.ANNUAL : terms.basis();
    if (!rateable(account, product, basis)) {
      return;
    }
    Rating strict = rating.rate(query(account, product, terms, basis));
    Integer priced = account.getProductVersionNo();
    if (priced != null && !priced.equals(strict.schemeVersion())) {
      throw new BusinessRuleException(
          RateSchemeExceptionService.NOT_CURRENT,
          "Account "
              + account.getArn()
              + " is priced on version "
              + priced
              + " of "
              + product.getCode()
              + "; version "
              + strict.schemeVersion()
              + " is current: update the account to re-price it or request a rate exception");
    }
    account.stampScheme(strict.schemeVersion(), strict.overrideRef());
  }

  private RatingQuery query(Account account, RiskProduct product, Terms terms, PeriodBasis basis) {
    Purpose purpose =
        account.getBusinessType() == BusinessType.RENEWAL ? Purpose.RENEWAL : Purpose.NEW_BUSINESS;
    String override =
        account.getRateOverrideRef() != null
            ? account.getRateOverrideRef()
            : exceptions.latestApproved(account.getArn(), product.getCode());
    return new RatingQuery(
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
            null)
        .withScheme(
            purpose, purpose == Purpose.RENEWAL ? account.getProductVersionNo() : null, override);
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
