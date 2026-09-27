package com.iortatechnxt.brokerverse.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Premium of a booked invoice by component, scale 2 (BRNB.027). Negative amounts are return premium
 * (negative endorsements, cancellations).
 *
 * @param basic basic (net) premium
 * @param dst documentary stamp tax
 * @param premiumTaxVat premium tax or VAT on premium
 * @param lgt local government tax
 * @param fst fire service tax
 * @param other other charges
 */
@Embeddable
public record PremiumComponents(
    @Column(name = "basic_premium", nullable = false, precision = 19, scale = 2) BigDecimal basic,
    @Column(name = "dst", nullable = false, precision = 19, scale = 2) BigDecimal dst,
    @Column(name = "premium_tax_vat", nullable = false, precision = 19, scale = 2)
        BigDecimal premiumTaxVat,
    @Column(name = "lgt", nullable = false, precision = 19, scale = 2) BigDecimal lgt,
    @Column(name = "fst", nullable = false, precision = 19, scale = 2) BigDecimal fst,
    @Column(name = "other_charges", nullable = false, precision = 19, scale = 2) BigDecimal other) {

  private static final int SCALE = 2;

  /** No premium. */
  public static final PremiumComponents ZERO =
      new PremiumComponents(
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.ZERO);

  /** Every amount at scale 2; null becomes zero. */
  public PremiumComponents {
    basic = money(basic);
    dst = money(dst);
    premiumTaxVat = money(premiumTaxVat);
    lgt = money(lgt);
    fst = money(fst);
    other = money(other);
  }

  /**
   * Components from a map (missing components are zero).
   *
   * @param amounts amount per component
   * @return components
   */
  public static PremiumComponents of(Map<PremiumComponent, BigDecimal> amounts) {
    return new PremiumComponents(
        amounts.get(PremiumComponent.BASIC),
        amounts.get(PremiumComponent.DST),
        amounts.get(PremiumComponent.PREMIUM_TAX_OR_VAT),
        amounts.get(PremiumComponent.LGT),
        amounts.get(PremiumComponent.FST),
        amounts.get(PremiumComponent.OTHER));
  }

  /**
   * The amounts by component, in component order.
   *
   * @return map of every component
   */
  public Map<PremiumComponent, BigDecimal> asMap() {
    Map<PremiumComponent, BigDecimal> map = new EnumMap<>(PremiumComponent.class);
    map.put(PremiumComponent.BASIC, basic);
    map.put(PremiumComponent.DST, dst);
    map.put(PremiumComponent.PREMIUM_TAX_OR_VAT, premiumTaxVat);
    map.put(PremiumComponent.LGT, lgt);
    map.put(PremiumComponent.FST, fst);
    map.put(PremiumComponent.OTHER, other);
    return map;
  }

  /**
   * Gross premium: the sum of the components.
   *
   * @return total
   */
  public BigDecimal total() {
    return basic.add(dst).add(premiumTaxVat).add(lgt).add(fst).add(other);
  }

  /**
   * The same components with the opposite sign.
   *
   * @return negated components
   */
  public PremiumComponents negate() {
    return map(BigDecimal::negate);
  }

  /**
   * Each component times a factor (share, refund factor), rounded half-up to centavos.
   *
   * @param factor factor
   * @return scaled components
   */
  public PremiumComponents times(BigDecimal factor) {
    return map(a -> a.multiply(factor).setScale(SCALE, RoundingMode.HALF_UP));
  }

  /**
   * Component-wise sum.
   *
   * @param other components to add
   * @return sum
   */
  public PremiumComponents plus(PremiumComponents other) {
    return new PremiumComponents(
        basic.add(other.basic),
        dst.add(other.dst),
        premiumTaxVat.add(other.premiumTaxVat),
        lgt.add(other.lgt),
        fst.add(other.fst),
        this.other.add(other.other));
  }

  /**
   * Component-wise difference.
   *
   * @param other components to subtract
   * @return difference
   */
  public PremiumComponents minus(PremiumComponents other) {
    return plus(other.negate());
  }

  /**
   * The same components without documentary stamp tax (cancellation retaining DST).
   *
   * @return components with DST zero
   */
  public PremiumComponents withoutDst() {
    return new PremiumComponents(basic, BigDecimal.ZERO, premiumTaxVat, lgt, fst, other);
  }

  /**
   * Whether every component is zero.
   *
   * @return true when there is no premium
   */
  public boolean isZero() {
    return asMap().values().stream().allMatch(a -> a.signum() == 0);
  }

  private PremiumComponents map(UnaryOperator<BigDecimal> f) {
    return new PremiumComponents(
        f.apply(basic),
        f.apply(dst),
        f.apply(premiumTaxVat),
        f.apply(lgt),
        f.apply(fst),
        f.apply(other));
  }

  private static BigDecimal money(BigDecimal amount) {
    return amount == null
        ? BigDecimal.ZERO.setScale(SCALE)
        : amount.setScale(SCALE, RoundingMode.HALF_UP);
  }
}
