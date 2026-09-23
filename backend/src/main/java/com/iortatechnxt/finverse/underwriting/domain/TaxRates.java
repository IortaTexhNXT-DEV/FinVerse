package com.iortatechnxt.finverse.underwriting.domain;

import java.math.BigDecimal;

/**
 * Philippine non-life premium taxes, in percent of the company's net premium. All rates are
 * configurable per product (typical values: DST 12.5, VAT 12, LGT 0.75 depending on the LGU, FST 2
 * for fire business only, premium tax 2 for non-VAT registered insurers).
 *
 * @param dst documentary stamp tax %
 * @param vat value added tax %
 * @param lgt local government (premium) tax %
 * @param fst fire service tax % (fire only)
 * @param premiumTax premium tax %
 */
public record TaxRates(
    BigDecimal dst, BigDecimal vat, BigDecimal lgt, BigDecimal fst, BigDecimal premiumTax) {

  /**
   * No taxes (e.g. exempt business).
   *
   * @return zero rates
   */
  public static TaxRates none() {
    return new TaxRates(
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
  }
}
