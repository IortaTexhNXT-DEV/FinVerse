package com.iortatechnxt.finverse.tax.domain;

/**
 * Kinds of tax a {@link TaxCode} represents (Philippine non-life insurance).
 *
 * <ul>
 *   <li>{@link #VAT_OUTPUT}: 12 % VAT on premiums of VAT-registered business (NIRC Sec. 108).
 *   <li>{@link #VAT_INPUT}: 12 % VAT passed on by VAT-registered suppliers, creditable against
 *       output VAT.
 *   <li>{@link #VAT_ZERO_RATED} / {@link #VAT_EXEMPT}: 0 % classifications of sales and purchases
 *       shown separately on the 2550Q and the summary lists.
 *   <li>{@link #PREMIUM_TAX}: percentage tax on premiums of business not subject to VAT (2551Q).
 *   <li>{@link #DST}: documentary stamp tax on policies (Form 2000).
 *   <li>{@link #LGT}: local government (business) tax on premiums, paid to the LGU.
 *   <li>{@link #FST}: fire service tax on fire premiums, remitted for the Bureau of Fire
 *       Protection.
 *   <li>{@link #EWT}: creditable expanded withholding tax, one code per ATC (0619-E / 1601-EQ).
 * </ul>
 */
public enum TaxType {
  VAT_OUTPUT,
  VAT_INPUT,
  VAT_ZERO_RATED,
  VAT_EXEMPT,
  PREMIUM_TAX,
  DST,
  LGT,
  FST,
  EWT;

  /**
   * Whether the tax is levied on premiums and computed by the underwriting premium breakdown.
   *
   * @return true for DST, premium tax, LGT and FST
   */
  public boolean isPremiumLevy() {
    return this == PREMIUM_TAX || this == DST || this == LGT || this == FST;
  }
}
