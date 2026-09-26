package com.iortatechnxt.brokerverse.currency.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exchange rate types (Currency Rate Type Master). */
public enum RateType {
  /** Daily transaction (spot) rate used for postings. */
  SPOT,
  /** Month-end closing rate used for revaluation of monetary balances. */
  CLOSING,
  /** Period average rate used for translation of income statement items. */
  AVERAGE,
  /** Planning rate used for budgets. */
  BUDGET,
  /**
   * Comptrollership book rate of Operations postings, kept with 2 decimals (CSHID.012-014, OQ08;
   * parameter {@code OPS_BOOK_RATE_TYPE}).
   */
  BOOK;

  /** Decimals of a {@link #BOOK} rate. */
  public static final int BOOK_RATE_SCALE = 2;

  /**
   * A rate at the precision of this type: {@link #BOOK} rates are rounded half-up to 2 decimals,
   * other types are kept as given.
   *
   * @param rate rate
   * @return rate at the type's precision
   */
  public BigDecimal normalize(BigDecimal rate) {
    return this == BOOK && rate != null
        ? rate.setScale(BOOK_RATE_SCALE, RoundingMode.HALF_UP)
        : rate;
  }
}
