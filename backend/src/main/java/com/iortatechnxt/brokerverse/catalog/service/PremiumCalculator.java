package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.catalog.domain.RatingMethod;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown.ItemPremium;
import com.iortatechnxt.brokerverse.catalog.service.PremiumRequest.Period;
import com.iortatechnxt.brokerverse.catalog.service.PremiumRequest.RatedItem;
import com.iortatechnxt.brokerverse.catalog.service.PremiumRequest.RatingRates;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Appendix A premium calculator: a pure function of a {@link PremiumRequest} whose rates are
 * already resolved (see {@code RatingService}). Used by accounts and, later, quotations.
 *
 * <ul>
 *   <li><b>Fire</b> ({@link RatingMethod#PROPERTY}) and <b>other lines</b> ({@link
 *       RatingMethod#GENERIC}): net = sum of item sum insured x rate.
 *   <li><b>Motor</b>: OD/Theft coverage = TSI x factor (90 % annual, 81 % multi-year, Q35); OD/PV
 *       rate = premium rate / 100; OD/Theft premium = coverage x OD/PV rate; basic premium =
 *       OD/Theft + excess BI + PD premiums.
 *   <li><b>Period</b>: pro-rata = days covered / days of the policy year starting on the inception
 *       date (365, or 366 when it contains 29 February); short period = table percentage.
 *   <li><b>Minimum premium</b>: replaces a lower net premium, except for endorsements and pro-rata.
 *   <li><b>Endorsements</b>: rated against the remaining term ({@link
 *       PremiumRequest.Period#remainingTerm}, pro-rata or short period); a reduction of the sum
 *       insured gives a negative (return) premium; no minimum premium.
 *   <li><b>Charges</b> on the net (basic) premium: DST with the Appendix A half-peso rounding,
 *       premium tax, VAT, FST and LGT at the insurer branch rate; gross = net + charges.
 *   <li><b>Commission</b> = rate x net premium; VAT on commission.
 * </ul>
 *
 * <p>Amounts are rounded to centavos half-up at each component, as on the insurer's computation.
 */
public final class PremiumCalculator {

  private static final int SCALE = 2;
  private static final int FACTOR_SCALE = 10;
  private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final BigDecimal HALF = new BigDecimal("0.50");
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);

  /**
   * Computes the breakdown.
   *
   * @param request items, rates, period and minimum premium
   * @return breakdown
   */
  public PremiumBreakdown calculate(PremiumRequest request) {
    if (request.items().isEmpty()) {
      throw new BusinessRuleException("RATING_NO_ITEMS", "Enter at least one item to rate");
    }
    RatingRates rates = request.rates();
    Base base =
        request.method() == RatingMethod.MOTOR
            ? motorBase(
                request.items(), odFactor(rates, request.multiYear()), request.endorsement())
            : standardBase(request.items(), request.endorsement());
    Period period = request.period() == null ? Period.ANNUAL : request.period();
    BigDecimal factor = periodFactor(period);
    BigDecimal adjusted = money(base.annual().multiply(factor));
    boolean minimumApplies =
        !request.endorsement()
            && period.basis() != PeriodBasis.PRO_RATA
            && request.minimumPremium() != null
            && adjusted.compareTo(request.minimumPremium()) < 0;
    BigDecimal net = minimumApplies ? money(request.minimumPremium()) : adjusted;
    BigDecimal dst = roundDst(percentOf(net, rates.dst()));
    BigDecimal premiumTax = money(percentOf(net, rates.premiumTax()));
    BigDecimal vat = money(percentOf(net, rates.vatPremium()));
    BigDecimal fst = money(percentOf(net, rates.fireServiceTax()));
    BigDecimal lgt = money(percentOf(net, rates.lgt()));
    BigDecimal charges = dst.add(premiumTax).add(vat).add(fst).add(lgt);
    BigDecimal commission = money(percentOf(net, rates.commission()));
    return new PremiumBreakdown(
        request.method(),
        base.sumInsured(),
        base.odCoverage(),
        base.odPremium(),
        base.bi(),
        base.pd(),
        base.annual(),
        factor,
        minimumApplies,
        net,
        dst,
        premiumTax,
        vat,
        fst,
        lgt,
        charges,
        net.add(charges),
        commission,
        money(percentOf(commission, rates.vatCommission())),
        base.items());
  }

  private static Base standardBase(List<RatedItem> items, boolean endorsement) {
    BigDecimal sum = ZERO;
    BigDecimal annual = ZERO;
    List<ItemPremium> priced = new ArrayList<>();
    for (RatedItem item : items) {
      BigDecimal premium = money(percentOf(requireSum(item, endorsement), item.ratePercent()));
      sum = sum.add(money(item.sumInsured()));
      annual = annual.add(premium);
      priced.add(new ItemPremium(item.label(), item.sumInsured(), item.ratePercent(), premium));
    }
    return new Base(sum, ZERO, ZERO, ZERO, ZERO, annual, priced);
  }

  private static Base motorBase(List<RatedItem> items, BigDecimal odFactor, boolean endorsement) {
    BigDecimal sum = ZERO;
    BigDecimal coverage = ZERO;
    BigDecimal odPremium = ZERO;
    BigDecimal bi = ZERO;
    BigDecimal pd = ZERO;
    List<ItemPremium> priced = new ArrayList<>();
    for (RatedItem item : items) {
      BigDecimal itemCoverage = money(percentOf(requireSum(item, endorsement), odFactor));
      BigDecimal itemOd = money(percentOf(itemCoverage, item.ratePercent()));
      BigDecimal itemBi = money(item.biPremium());
      BigDecimal itemPd = money(item.pdPremium());
      sum = sum.add(money(item.sumInsured()));
      coverage = coverage.add(itemCoverage);
      odPremium = odPremium.add(itemOd);
      bi = bi.add(itemBi);
      pd = pd.add(itemPd);
      priced.add(
          new ItemPremium(
              item.label(), item.sumInsured(), item.ratePercent(), itemOd.add(itemBi).add(itemPd)));
    }
    return new Base(sum, coverage, odPremium, bi, pd, odPremium.add(bi).add(pd), priced);
  }

  private static BigDecimal odFactor(RatingRates rates, boolean multiYear) {
    BigDecimal factor = multiYear ? rates.odMultiYear() : rates.odAnnual();
    if (factor == null) {
      throw new BusinessRuleException(
          "RATING_OD_FACTOR_MISSING", "The motor OD/Theft coverage factor is not set up");
    }
    return factor;
  }

  /**
   * The sum insured of an item: zero or more, or negative for an endorsement that reduces the cover
   * (return premium).
   */
  private static BigDecimal requireSum(RatedItem item, boolean endorsement) {
    if (item.sumInsured() == null || item.sumInsured().signum() < 0 && !endorsement) {
      throw new BusinessRuleException(
          "RATING_SUM_INSURED_INVALID", "Enter a sum insured of zero or more for every item");
    }
    return item.sumInsured();
  }

  /**
   * Factor applied to the annual premium for the period covered.
   *
   * @param period period basis and dates
   * @return factor (1 for annual), scale 10
   */
  static BigDecimal periodFactor(Period period) {
    return switch (period.basis()) {
      case ANNUAL -> BigDecimal.ONE;
      case PRO_RATA -> proRataFactor(period.from(), period.to());
      case SHORT_PERIOD -> shortPeriodFactor(period.shortPeriodPercent());
    };
  }

  /**
   * Pro-rata factor: days covered divided by the days of the policy year that starts on the
   * inception date (366 when that year contains 29 February, else 365).
   *
   * @param from period start
   * @param to period end (exclusive of cover after this date)
   * @return factor, scale 10
   */
  public static BigDecimal proRataFactor(LocalDate from, LocalDate to) {
    if (from == null || to == null || !to.isAfter(from)) {
      throw new BusinessRuleException(
          "RATING_PERIOD_INVALID", "The period end must be after the period start");
    }
    long days = ChronoUnit.DAYS.between(from, to);
    long yearDays = ChronoUnit.DAYS.between(from, from.plusYears(1));
    return BigDecimal.valueOf(days).divide(BigDecimal.valueOf(yearDays), FACTOR_SCALE, ROUNDING);
  }

  private static BigDecimal shortPeriodFactor(BigDecimal percent) {
    if (percent == null || percent.signum() <= 0 || percent.compareTo(HUNDRED) > 0) {
      throw new BusinessRuleException(
          "RATING_SHORT_PERIOD_INVALID", "No short-period percentage applies to this period");
    }
    return percent.divide(HUNDRED, FACTOR_SCALE, ROUNDING);
  }

  /**
   * Appendix A documentary stamp tax rounding. The tax is first rounded to centavos; then a centavo
   * part of .01 to .49 becomes .50 and .51 to .99 becomes the next peso, while exactly .00 and .50
   * stay: the amount is rounded up to the next half peso. Negative amounts (refunds) are rounded
   * the same way on their absolute value.
   *
   * @param amount computed tax
   * @return rounded tax, scale 2
   */
  public static BigDecimal roundDst(BigDecimal amount) {
    BigDecimal centavos = money(amount);
    BigDecimal magnitude = centavos.abs();
    BigDecimal halves = magnitude.divide(HALF, 0, RoundingMode.CEILING);
    BigDecimal rounded = halves.multiply(HALF).setScale(SCALE, ROUNDING);
    return centavos.signum() < 0 ? rounded.negate() : rounded;
  }

  private static BigDecimal percentOf(BigDecimal amount, BigDecimal percent) {
    if (amount == null || percent == null) {
      return BigDecimal.ZERO;
    }
    return amount.multiply(percent).divide(HUNDRED, FACTOR_SCALE, ROUNDING);
  }

  private static BigDecimal money(BigDecimal amount) {
    return amount == null ? ZERO : amount.setScale(SCALE, ROUNDING);
  }

  /** Annual premium base before the period and minimum adjustments. */
  private record Base(
      BigDecimal sumInsured,
      BigDecimal odCoverage,
      BigDecimal odPremium,
      BigDecimal bi,
      BigDecimal pd,
      BigDecimal annual,
      List<ItemPremium> items) {}
}
