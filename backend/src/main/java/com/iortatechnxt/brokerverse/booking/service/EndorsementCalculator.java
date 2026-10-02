package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.booking.domain.CancellationKind;
import com.iortatechnxt.brokerverse.booking.domain.CommissionTerms;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponents;
import com.iortatechnxt.brokerverse.catalog.domain.RateCode;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown;
import com.iortatechnxt.brokerverse.catalog.service.PremiumCalculator;
import com.iortatechnxt.brokerverse.catalog.service.RateResolver;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery;
import com.iortatechnxt.brokerverse.catalog.service.RatingService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Amounts of endorsements and cancellations (BRNB.076/081/094) with the catalog calculator
 * (Appendix A):
 *
 * <ul>
 *   <li>a financial endorsement rates the sum insured change for the remaining term of the policy
 *       year ({@code PremiumRequest.Period.remainingTerm}: pro-rata days or short-period table; no
 *       minimum premium; a negative change gives return premium), or takes the components given;
 *   <li>a cancellation returns the premium in force for the policy year: everything (FLAT), all but
 *       DST (FLAT_RETAIN_DST), or the unexpired part with DST retained (PARTIAL) - pro-rata on the
 *       days left, or short-period where the insurer keeps the table percentage of the months
 *       elapsed.
 * </ul>
 */
@Component
public class EndorsementCalculator {

  private static final int FACTOR_SCALE = 10;
  private static final int MONEY_SCALE = 2;
  private static final int MONTHS_IN_YEAR = 12;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final RatingService rating;
  private final RateResolver resolver;
  private final BookingSettings settings;

  /**
   * Creates the calculator.
   *
   * @param rating catalog rating
   * @param resolver rates (VAT on commission, short-period table)
   * @param settings withholding tax rate
   */
  public EndorsementCalculator(
      RatingService rating, RateResolver resolver, BookingSettings settings) {
    this.rating = rating;
    this.resolver = resolver;
    this.settings = settings;
  }

  /**
   * Premium and commission of a financial endorsement.
   *
   * @param account account
   * @param year policy year concerned
   * @param posting endorsement
   * @return signed amounts
   */
  public Amounts financial(Account account, PolicyYear year, EndorsementPosting posting) {
    if (posting.premium() != null) {
      CommissionTerms commission =
          posting.commission() != null
              ? posting.commission()
              : derivedCommission(
                  account, year, posting.premium().basic(), posting.effectiveDate());
      return new Amounts(posting.premium(), commission);
    }
    BigDecimal change = posting.sumInsuredChange();
    if (change == null || change.signum() == 0) {
      throw new BusinessRuleException(
          "ENDORSEMENT_CHANGE_REQUIRED", "Enter the change of the sum insured or the premium");
    }
    PremiumBreakdown b =
        rating
            .rate(
                new RatingQuery(
                    account.getCompanyId(),
                    account.getProductCode(),
                    account.getInsurerCode(),
                    account.getInsurerBranch(),
                    List.of(
                        new RatingQuery.Item(
                            "Endorsement", change, posting.ratePercent(), null, null)),
                    account.isMultiYear(),
                    posting.basisOrDefault(),
                    posting.effectiveDate(),
                    year.expiry(),
                    year.commission().rate(),
                    true,
                    posting.effectiveDate(),
                    RatingQuery.Purpose.ENDORSEMENT,
                    account.getProductVersionNo(),
                    null))
            .breakdown();
    PremiumComponents premium =
        new PremiumComponents(
            b.netPremium(),
            b.dst(),
            b.premiumTax().add(b.vat()),
            b.lgt(),
            b.fst(),
            BigDecimal.ZERO);
    return new Amounts(
        premium,
        CommissionTerms.of(
            year.commission().rate(), b.commission(), b.vatOnCommission(), settings.wtaxRate()));
  }

  /**
   * Return premium and commission of a cancellation of the policy year.
   *
   * @param year policy year with the amounts in force
   * @param kind cancellation kind
   * @param basis PRO_RATA or SHORT_PERIOD (partial)
   * @param effective cancellation date
   * @return negative amounts
   */
  public Amounts cancellation(
      PolicyYear year, CancellationKind kind, PeriodBasis basis, LocalDate effective) {
    PremiumComponents base =
        kind == CancellationKind.FLAT ? year.premium() : year.premium().withoutDst();
    if (kind != CancellationKind.PARTIAL) {
      return new Amounts(base.negate(), year.commission().negate());
    }
    BigDecimal factor = refundFactor(year, basis, effective);
    return new Amounts(base.times(factor).negate(), year.commission().times(factor).negate());
  }

  /**
   * Share of the policy year's premium returned on a partial cancellation.
   *
   * @param year policy year
   * @param basis PRO_RATA or SHORT_PERIOD
   * @param effective cancellation date
   * @return factor between 0 and 1
   */
  BigDecimal refundFactor(PolicyYear year, PeriodBasis basis, LocalDate effective) {
    if (!effective.isAfter(year.inception())) {
      return BigDecimal.ONE;
    }
    if (basis == PeriodBasis.SHORT_PERIOD) {
      int months = monthsElapsed(year.inception(), effective);
      BigDecimal retained =
          resolver
              .shortPeriodPercent(months, effective)
              .orElseThrow(
                  () ->
                      new BusinessRuleException(
                          "SHORT_PERIOD_RATE_MISSING",
                          "No short-period rate is set up for " + months + " month(s)"));
      return BigDecimal.ONE
          .subtract(retained.divide(HUNDRED, FACTOR_SCALE, RoundingMode.HALF_UP))
          .max(BigDecimal.ZERO);
    }
    return PremiumCalculator.proRataFactor(effective, year.expiry()).min(BigDecimal.ONE);
  }

  private CommissionTerms derivedCommission(
      Account account, PolicyYear year, BigDecimal basic, LocalDate date) {
    BigDecimal rate = year.commission().rate();
    BigDecimal commission = basic.multiply(rate).divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
    BigDecimal vatRate =
        resolver.rate(RateCode.VAT_COMMISSION, account.getLineCode(), date).orElse(BigDecimal.ZERO);
    BigDecimal vat =
        commission.multiply(vatRate).divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
    return CommissionTerms.of(rate, commission, vat, settings.wtaxRate());
  }

  private static int monthsElapsed(LocalDate from, LocalDate to) {
    long months = ChronoUnit.MONTHS.between(from, to);
    if (from.plusMonths(months).isBefore(to)) {
      months++;
    }
    return (int) Math.min(MONTHS_IN_YEAR, Math.max(1, months));
  }

  /**
   * A policy year with the premium and commission in force (every booked invoice of the year).
   *
   * @param year policy year
   * @param inception start of the year
   * @param expiry end of the year
   * @param premium premium in force
   * @param commission commission in force
   */
  public record PolicyYear(
      int year,
      LocalDate inception,
      LocalDate expiry,
      PremiumComponents premium,
      CommissionTerms commission) {}

  /**
   * Signed amounts of an endorsement.
   *
   * @param premium premium by component
   * @param commission commission terms
   */
  public record Amounts(PremiumComponents premium, CommissionTerms commission) {}
}
