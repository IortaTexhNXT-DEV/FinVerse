package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.catalog.domain.MotorCoverage;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLine;
import com.iortatechnxt.brokerverse.catalog.domain.RateCode;
import com.iortatechnxt.brokerverse.catalog.domain.RatingMethod;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.SchemeTerms;
import com.iortatechnxt.brokerverse.catalog.domain.ShortPeriodRate;
import com.iortatechnxt.brokerverse.catalog.service.PremiumRequest.Period;
import com.iortatechnxt.brokerverse.catalog.service.PremiumRequest.RatedItem;
import com.iortatechnxt.brokerverse.catalog.service.PremiumRequest.RatingRates;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rates a product for accounts, quotations and the premium calculator screen: resolves the rates in
 * force (taxes per line, LGT of the insurer branch, commission of the insurer or the product
 * default, motor limits, short-period table) and runs the Appendix A {@link PremiumCalculator}.
 *
 * <p>Package products are priced on the rate scheme of the package version that the purpose selects
 * ({@link SchemeResolver}, BRPM.007): for new business always the current released version,
 * whatever the period start. Statutory rates keep their effective-date look-up on the period start
 * (PQ10). {@link #rate} refuses an item rate other than the scheme rate without an approved
 * exception; {@link #rateDraft} prices it and reports the deviation, so that a draft can be saved
 * and the exception requested before submission.
 */
@Service
@Transactional(readOnly = true)
public class RatingService {

  private final ProductCatalogService catalog;
  private final InsurerService insurers;
  private final RateResolver resolver;
  private final SchemeResolver schemes;
  private final Clock clock;
  private final PremiumCalculator calculator = new PremiumCalculator();

  /**
   * Creates the service.
   *
   * @param catalog products and lines
   * @param insurers insurers and branches
   * @param resolver rate lookups
   * @param schemes package rate schemes
   * @param clock clock
   */
  public RatingService(
      ProductCatalogService catalog,
      InsurerService insurers,
      RateResolver resolver,
      SchemeResolver schemes,
      Clock clock) {
    this.catalog = catalog;
    this.insurers = insurers;
    this.resolver = resolver;
    this.schemes = schemes;
    this.clock = clock;
  }

  /**
   * Rates a product; for new business on a package, an item rate other than the scheme rate needs
   * an approved exception ({@code RATE_SCHEME_NOT_CURRENT}, BRPM.007).
   *
   * @param query product, insurer branch, items, period, purpose and version
   * @return breakdown with the rates and the scheme version used
   */
  public Rating rate(RatingQuery query) {
    return rate(query, true);
  }

  /**
   * Rates a draft: like {@link #rate} but an item rate other than the scheme rate is priced and
   * reported ({@link Rating#schemeDeviation()}) instead of refused. Version and sellability rules
   * still apply.
   *
   * @param query product, insurer branch, items, period, purpose and version
   * @return breakdown with the rates and the scheme version used
   */
  public Rating rateDraft(RatingQuery query) {
    return rate(query, false);
  }

  private Rating rate(RatingQuery query, boolean strict) {
    RiskProduct product = catalog.requireUsableProduct(query.productCode());
    ProductLine line = catalog.requireLine(product.getLineCode());
    LocalDate date = ratingDate(query);
    SchemeResolver.Scheme scheme = schemes.resolve(product, query, date);
    boolean newBusiness = query.purpose() == RatingQuery.Purpose.NEW_BUSINESS;
    List<BigDecimal> deviating = newBusiness ? scheme.deviatingRates(query) : List.of();
    String overrideRef = scheme.overrideRef();
    if (strict && !deviating.isEmpty()) {
      overrideRef = schemes.requireSchemeRates(scheme, query);
    }
    RatingRates rates = rates(query, product, scheme, date);
    Period period = period(query, date);
    List<RatedItem> items =
        query.items().stream().map(i -> rated(i, scheme.rate(), line, date)).toList();
    PremiumBreakdown breakdown =
        calculator.calculate(
            new PremiumRequest(
                line.getRatingMethod(),
                items,
                rates,
                query.multiYear(),
                period,
                scheme.minimumPremium(),
                query.endorsement()));
    return new Rating(
        breakdown,
        rates,
        period.basis(),
        period.shortPeriodPercent(),
        scheme.versionNo(),
        scheme.rate(),
        overrideRef,
        !deviating.isEmpty() || scheme.nonCurrent());
  }

  /**
   * The test premium of a package version at validation (PMADD06): one item of the given sum
   * insured at the scheme rate, annual, with the statutory rates in force today and no insurer.
   *
   * @param productCode risk code
   * @param terms rate scheme of the version
   * @param sumInsured sample sum insured
   * @return gross premium, empty when the scheme has no rate
   */
  public Optional<BigDecimal> testPremium(
      String productCode, SchemeTerms terms, BigDecimal sumInsured) {
    if (terms.defaultRate() == null) {
      return Optional.empty();
    }
    RiskProduct product = catalog.requireProduct(productCode);
    ProductLine line = catalog.requireLine(product.getLineCode());
    LocalDate today = LocalDate.now(clock);
    SchemeResolver.Scheme scheme =
        new SchemeResolver.Scheme(
            null,
            terms.defaultRate(),
            terms.minimumPremium(),
            terms.defaultCommissionRate(),
            true,
            null,
            false);
    RatingQuery query =
        new RatingQuery(
            null,
            productCode,
            null,
            null,
            List.of(new RatingQuery.Item("Test item", sumInsured, null, null, null)),
            false,
            PeriodBasis.ANNUAL,
            null,
            null,
            null,
            false,
            today);
    PremiumBreakdown breakdown =
        calculator.calculate(
            new PremiumRequest(
                line.getRatingMethod(),
                List.of(rated(query.items().get(0), scheme.rate(), line, today)),
                rates(query, product, scheme, today),
                false,
                Period.ANNUAL,
                scheme.minimumPremium(),
                false));
    return Optional.of(breakdown.grossPremium());
  }

  private LocalDate ratingDate(RatingQuery query) {
    if (query.ratingDate() != null) {
      return query.ratingDate();
    }
    return query.periodFrom() != null ? query.periodFrom() : LocalDate.now(clock);
  }

  private RatingRates rates(
      RatingQuery q, RiskProduct product, SchemeResolver.Scheme scheme, LocalDate date) {
    String line = product.getLineCode();
    BigDecimal lgt = BigDecimal.ZERO;
    if (q.insurerCode() != null && q.branchCode() != null) {
      lgt =
          insurers.requireUsableBranch(q.companyId(), q.insurerCode(), q.branchCode()).getLgtRate();
    }
    return new RatingRates(
        rate(RateCode.DST, line, date),
        rate(RateCode.PREMIUM_TAX, line, date),
        rate(RateCode.VAT_PREMIUM, line, date),
        rate(RateCode.FIRE_SERVICE_TAX, line, date),
        lgt,
        commission(q, product, scheme, date),
        rate(RateCode.VAT_COMMISSION, line, date),
        rate(RateCode.MOTOR_OD_ANNUAL, line, date),
        rate(RateCode.MOTOR_OD_MULTI_YEAR, line, date));
  }

  private BigDecimal rate(RateCode code, String line, LocalDate date) {
    return resolver.rate(code, line, date).orElse(BigDecimal.ZERO);
  }

  private BigDecimal commission(
      RatingQuery q, RiskProduct product, SchemeResolver.Scheme scheme, LocalDate date) {
    if (q.commissionRate() != null) {
      return q.commissionRate();
    }
    if (q.insurerCode() == null) {
      return scheme.commissionRate();
    }
    return resolver
        .commission(q.companyId(), q.insurerCode(), product.getCode(), date)
        .orElse(scheme.commissionRate());
  }

  private Period period(RatingQuery q, LocalDate date) {
    PeriodBasis basis = q.basis() == null ? PeriodBasis.ANNUAL : q.basis();
    return switch (basis) {
      case ANNUAL -> Period.ANNUAL;
      case PRO_RATA -> new Period(basis, q.periodFrom(), q.periodTo(), null);
      case SHORT_PERIOD -> {
        int months = monthsCovered(q.periodFrom(), q.periodTo());
        BigDecimal percent =
            resolver
                .shortPeriodPercent(months, date)
                .orElseThrow(
                    () ->
                        new BusinessRuleException(
                            "SHORT_PERIOD_RATE_MISSING",
                            "No short-period rate is set up for " + months + " month(s)"));
        yield new Period(basis, q.periodFrom(), q.periodTo(), percent);
      }
    };
  }

  /**
   * Months covered by a period, a started month counting as a full month (1 to 12).
   *
   * @param from start
   * @param to end
   * @return months
   */
  static int monthsCovered(LocalDate from, LocalDate to) {
    if (from == null || to == null || !to.isAfter(from)) {
      throw new BusinessRuleException(
          "RATING_PERIOD_INVALID", "The period end must be after the period start");
    }
    long months = ChronoUnit.MONTHS.between(from, to);
    if (from.plusMonths(months).isBefore(to)) {
      months++;
    }
    return (int) Math.min(ShortPeriodRate.MONTHS_IN_YEAR, Math.max(1, months));
  }

  private RatedItem rated(
      RatingQuery.Item item, BigDecimal schemeRate, ProductLine line, LocalDate d) {
    BigDecimal rate = item.ratePercent() != null ? item.ratePercent() : schemeRate;
    if (rate == null) {
      throw new BusinessRuleException(
          "RATING_RATE_REQUIRED", "Enter the premium rate of " + labelOf(item));
    }
    if (line.getRatingMethod() != RatingMethod.MOTOR) {
      return RatedItem.of(item.label(), item.sumInsured(), rate);
    }
    return new RatedItem(
        item.label(),
        item.sumInsured(),
        rate,
        limitPremium(MotorCoverage.BI, item.biLimit(), d),
        limitPremium(MotorCoverage.PD, item.pdLimit(), d));
  }

  private BigDecimal limitPremium(MotorCoverage coverage, BigDecimal limit, LocalDate date) {
    if (limit == null || limit.signum() == 0) {
      return BigDecimal.ZERO;
    }
    return resolver
        .motorLimitPremium(coverage, limit, date)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "MOTOR_LIMIT_UNKNOWN",
                    coverage + " limit " + limit.toPlainString() + " is not in the limit table"));
  }

  private static String labelOf(RatingQuery.Item item) {
    return item.label() == null || item.label().isBlank() ? "the item" : item.label();
  }

  /**
   * Rating outcome.
   *
   * @param breakdown Appendix A breakdown
   * @param rates rates used, in percent
   * @param basis period basis applied
   * @param shortPeriodPercent short-period percentage applied, null otherwise
   * @param schemeVersion package version that priced it, null for a product without versions
   *     (BRPM.007: stored on the quotation, account and invoice)
   * @param schemeRate scheme (or panel insurer) rate of that version, null when none
   * @param overrideRef approved rate-scheme exception used, null when none
   * @param schemeDeviation an item rate differs from the scheme rate, or a non-current version was
   *     used: submission needs an approved exception
   */
  public record Rating(
      PremiumBreakdown breakdown,
      RatingRates rates,
      PeriodBasis basis,
      BigDecimal shortPeriodPercent,
      Integer schemeVersion,
      BigDecimal schemeRate,
      String overrideRef,
      boolean schemeDeviation) {}
}
