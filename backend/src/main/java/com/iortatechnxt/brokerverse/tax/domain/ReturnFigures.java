package com.iortatechnxt.brokerverse.tax.domain;

import com.iortatechnxt.brokerverse.common.util.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Headline figures of a return.
 *
 * <p>Amount payable = tax due − credits when positive, else zero with the difference carried as an
 * excess credit (e.g. excess input VAT carried over to the next quarter, NIRC Sec. 110(B)). Credits
 * are input VAT and prior-quarter carry-over for VAT, and the monthly 0619-E remittances of the
 * quarter for 1601-EQ.
 *
 * @param taxBase amount the tax is computed on (taxable sales, income payments, premiums)
 * @param taxDue tax due before credits
 * @param taxCredits credits applied
 * @param amountPayable tax still payable (never negative)
 * @param excessCredit credits exceeding the tax due (never negative)
 */
public record ReturnFigures(
    BigDecimal taxBase,
    BigDecimal taxDue,
    BigDecimal taxCredits,
    BigDecimal amountPayable,
    BigDecimal excessCredit) {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  /**
   * Computes payable and excess credit.
   *
   * @param taxBase tax base
   * @param taxDue tax due
   * @param taxCredits credits
   * @return figures, rounded to centavos
   */
  public static ReturnFigures of(BigDecimal taxBase, BigDecimal taxDue, BigDecimal taxCredits) {
    BigDecimal due = Money.round(Money.nz(taxDue));
    BigDecimal credits = Money.round(Money.nz(taxCredits));
    BigDecimal net = due.subtract(credits);
    return new ReturnFigures(
        Money.round(Money.nz(taxBase)),
        due,
        credits,
        net.signum() > 0 ? net : Money.zero(),
        net.signum() < 0 ? net.negate() : Money.zero());
  }

  /**
   * Percentage of an amount, rounded half-even to centavos (the rounding used by the premium and
   * invoice calculators, so worksheet recomputations agree with the posted documents).
   *
   * @param amount amount
   * @param ratePercent rate in percent (e.g. 12 for 12 %)
   * @return amount × rate / 100
   */
  public static BigDecimal percentOf(BigDecimal amount, BigDecimal ratePercent) {
    return Money.round(
        Money.nz(amount)
            .multiply(Money.nz(ratePercent))
            .divide(HUNDRED, Money.RATE_SCALE, RoundingMode.HALF_EVEN));
  }

  /**
   * Effective rate of a tax on its base, in percent (for display and rate-mismatch checks).
   *
   * @param base tax base
   * @param tax tax
   * @return rate with 2 decimals, zero when the base is zero
   */
  public static BigDecimal effectiveRate(BigDecimal base, BigDecimal tax) {
    if (Money.nz(base).signum() == 0) {
      return BigDecimal.ZERO.setScale(2);
    }
    return Money.nz(tax).multiply(HUNDRED).divide(base, 2, RoundingMode.HALF_EVEN);
  }
}
