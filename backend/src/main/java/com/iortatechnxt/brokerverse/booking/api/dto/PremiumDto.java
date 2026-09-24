package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.PremiumComponents;
import java.math.BigDecimal;

/**
 * Premium by component.
 *
 * @param basic basic premium
 * @param dst documentary stamp tax
 * @param premiumTaxVat premium tax or VAT
 * @param lgt local government tax
 * @param fst fire service tax
 * @param other other charges
 * @param total gross premium (ignored on input)
 */
public record PremiumDto(
    BigDecimal basic,
    BigDecimal dst,
    BigDecimal premiumTaxVat,
    BigDecimal lgt,
    BigDecimal fst,
    BigDecimal other,
    BigDecimal total) {

  /**
   * Maps components.
   *
   * @param p components
   * @return DTO
   */
  public static PremiumDto from(PremiumComponents p) {
    return new PremiumDto(
        p.basic(), p.dst(), p.premiumTaxVat(), p.lgt(), p.fst(), p.other(), p.total());
  }

  /**
   * To the domain value.
   *
   * @return components
   */
  public PremiumComponents toComponents() {
    return new PremiumComponents(basic, dst, premiumTaxVat, lgt, fst, other);
  }
}
