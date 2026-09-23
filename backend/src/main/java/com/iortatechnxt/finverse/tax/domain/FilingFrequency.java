package com.iortatechnxt.finverse.tax.domain;

/**
 * How often a tax form is filed.
 *
 * <ul>
 *   <li>{@link #MONTHLY}: every calendar month (DST 2000, 1601-C, FST).
 *   <li>{@link #QUARTERLY}: every calendar quarter (2550Q, 1601-EQ, 2551Q, local business tax).
 *   <li>{@link #MONTHLY_EXCEPT_QUARTER_END}: first two months of each quarter only; the third month
 *       is covered by the quarterly return (0619-E remittance form).
 *   <li>{@link #ANNUAL}: once per calendar year.
 * </ul>
 */
public enum FilingFrequency {
  MONTHLY,
  QUARTERLY,
  MONTHLY_EXCEPT_QUARTER_END,
  ANNUAL
}
