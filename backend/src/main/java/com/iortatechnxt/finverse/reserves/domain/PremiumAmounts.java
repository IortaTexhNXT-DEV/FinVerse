package com.iortatechnxt.finverse.reserves.domain;

import com.iortatechnxt.finverse.common.util.Money;
import java.math.BigDecimal;

/**
 * Premium and commission amounts of one premium transaction (company share, base currency): either
 * as written, or their unearned part at a valuation date.
 *
 * @param premium premium (company net premium, gross of reinsurance)
 * @param commission intermediary commission (acquisition cost)
 * @param treatyPremium premium ceded to treaties
 * @param facPremium premium ceded facultatively
 * @param riCommission reinsurance commission receivable on the ceded premium
 */
public record PremiumAmounts(
    BigDecimal premium,
    BigDecimal commission,
    BigDecimal treatyPremium,
    BigDecimal facPremium,
    BigDecimal riCommission) {

  /**
   * All amounts zero.
   *
   * @return zero amounts
   */
  public static PremiumAmounts zero() {
    return new PremiumAmounts(Money.zero(), Money.zero(), Money.zero(), Money.zero(), Money.zero());
  }

  /**
   * Every amount multiplied by a fraction (e.g. the unearned share), rounded to cents.
   *
   * @param fraction fraction
   * @return scaled amounts
   */
  public PremiumAmounts times(BigDecimal fraction) {
    return new PremiumAmounts(
        Money.round(premium.multiply(fraction)),
        Money.round(commission.multiply(fraction)),
        Money.round(treatyPremium.multiply(fraction)),
        Money.round(facPremium.multiply(fraction)),
        Money.round(riCommission.multiply(fraction)));
  }

  /**
   * Sum of two sets of amounts.
   *
   * @param other other amounts
   * @return sum
   */
  public PremiumAmounts plus(PremiumAmounts other) {
    return new PremiumAmounts(
        premium.add(other.premium),
        commission.add(other.commission),
        treatyPremium.add(other.treatyPremium),
        facPremium.add(other.facPremium),
        riCommission.add(other.riCommission));
  }

  /**
   * Difference of two sets of amounts.
   *
   * @param other amounts to subtract
   * @return this − other
   */
  public PremiumAmounts minus(PremiumAmounts other) {
    return plus(other.times(BigDecimal.ONE.negate()));
  }

  /**
   * Premium ceded to reinsurers.
   *
   * @return treaty + FAC premium
   */
  public BigDecimal cededPremium() {
    return treatyPremium.add(facPremium);
  }
}
