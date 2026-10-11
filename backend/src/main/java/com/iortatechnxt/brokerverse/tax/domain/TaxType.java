package com.iortatechnxt.brokerverse.tax.domain;

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
 *   <li>{@link #FWT}: final withholding tax, one code per ATC: tax withheld by government payors on
 *       payments to the company, final tax on bank interest.
 *   <li>{@link #FINAL_VAT}: final VAT withheld by government payors on the company's sales.
 *   <li>{@link #PERCENTAGE_TAX}: percentage tax on the company's own receipts not subject to VAT.
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
  EWT,
  FWT,
  FINAL_VAT,
  PERCENTAGE_TAX;

  /**
   * Whether the tax is levied on premiums and computed by the underwriting premium breakdown.
   *
   * @return true for DST, premium tax, LGT and FST
   */
  public boolean isPremiumLevy() {
    return this == PREMIUM_TAX || this == DST || this == LGT || this == FST;
  }

  /**
   * Whether the tax is a withholding identified by its ATC (creditable or final).
   *
   * @return true for EWT and FWT
   */
  public boolean needsAtc() {
    return this == EWT || this == FWT;
  }

  /**
   * Whether the tax withheld is final, not creditable against the company's income tax or VAT.
   *
   * @return true for FWT and FINAL_VAT
   */
  public boolean isFinal() {
    return this == FWT || this == FINAL_VAT;
  }
}
