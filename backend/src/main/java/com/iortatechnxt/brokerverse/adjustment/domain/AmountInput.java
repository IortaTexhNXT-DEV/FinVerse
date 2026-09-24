package com.iortatechnxt.brokerverse.adjustment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.stream.Stream;

/**
 * Signed changes entered for an amount change (premium rate or amount, charges, commission;
 * ADJID.002/014). Null means no change; the commission is derived from the basic premium change
 * when not given.
 *
 * @param basic basic premium change
 * @param dst documentary stamp tax change
 * @param premiumTaxVat premium tax / VAT change
 * @param lgt local government tax change
 * @param fst fire service tax change
 * @param other other charges change
 * @param commission commission change, null to derive it
 * @param vatOnCommission VAT on commission change, null to derive it
 */
@Embeddable
public record AmountInput(
    @Column(name = "in_basic", precision = 19, scale = 2) BigDecimal basic,
    @Column(name = "in_dst", precision = 19, scale = 2) BigDecimal dst,
    @Column(name = "in_premium_tax_vat", precision = 19, scale = 2) BigDecimal premiumTaxVat,
    @Column(name = "in_lgt", precision = 19, scale = 2) BigDecimal lgt,
    @Column(name = "in_fst", precision = 19, scale = 2) BigDecimal fst,
    @Column(name = "in_other", precision = 19, scale = 2) BigDecimal other,
    @Column(name = "in_commission", precision = 19, scale = 2) BigDecimal commission,
    @Column(name = "in_vat_on_commission", precision = 19, scale = 2) BigDecimal vatOnCommission) {

  /** No amounts entered. */
  public static final AmountInput NONE =
      new AmountInput(null, null, null, null, null, null, null, null);

  /**
   * Whether any premium component is entered with a non-zero value.
   *
   * @return true when the premium changes
   */
  public boolean changesPremium() {
    return Stream.of(basic, dst, premiumTaxVat, lgt, fst, other)
        .anyMatch(a -> a != null && a.signum() != 0);
  }

  /**
   * Whether a non-zero commission change is entered.
   *
   * @return true when the commission changes explicitly
   */
  public boolean changesCommission() {
    return commission != null && commission.signum() != 0;
  }
}
