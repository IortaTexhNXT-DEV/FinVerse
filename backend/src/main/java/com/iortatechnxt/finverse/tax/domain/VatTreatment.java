package com.iortatechnxt.finverse.tax.domain;

/**
 * VAT treatment of a customer, used to classify premiums on which no VAT was charged.
 *
 * <ul>
 *   <li>{@link #REGULAR}: premiums with VAT are taxable sales; without VAT they are exempt (e.g.
 *       business subject to premium tax instead of VAT).
 *   <li>{@link #ZERO_RATED}: premiums without VAT are zero-rated sales (e.g. PEZA or BOI-registered
 *       export enterprises, international carriers).
 *   <li>{@link #EXEMPT}: premiums without VAT are exempt sales.
 * </ul>
 */
public enum VatTreatment {
  REGULAR,
  ZERO_RATED,
  EXEMPT
}
