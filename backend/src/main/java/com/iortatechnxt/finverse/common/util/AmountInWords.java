package com.iortatechnxt.finverse.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Spells an amount in English words for voucher and cheque prints, e.g. {@code 1250.50} in PHP
 * becomes "Philippine Peso One Thousand Two Hundred Fifty and 50/100 only".
 */
public final class AmountInWords {

  private static final String[] UNITS = {
    "Zero",
    "One",
    "Two",
    "Three",
    "Four",
    "Five",
    "Six",
    "Seven",
    "Eight",
    "Nine",
    "Ten",
    "Eleven",
    "Twelve",
    "Thirteen",
    "Fourteen",
    "Fifteen",
    "Sixteen",
    "Seventeen",
    "Eighteen",
    "Nineteen"
  };
  private static final String[] TENS = {
    "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
  };
  private static final String[] SCALES = {"", " Thousand", " Million", " Billion", " Trillion"};
  private static final int THOUSAND = 1000;
  private static final int HUNDRED = 100;
  private static final int TWENTY = 20;
  private static final int TEN = 10;

  private AmountInWords() {}

  /**
   * Spells an amount.
   *
   * @param amount amount (sign ignored)
   * @param currencyName currency name printed first, e.g. "Philippine Peso"
   * @return words
   */
  public static String spell(BigDecimal amount, String currencyName) {
    BigDecimal abs = amount.abs().setScale(2, RoundingMode.HALF_UP);
    long whole = abs.longValue();
    int cents = abs.subtract(BigDecimal.valueOf(whole)).movePointRight(2).intValue();
    return currencyName + " " + words(whole) + " and " + String.format("%02d/100 only", cents);
  }

  /**
   * Spells a non-negative whole number.
   *
   * @param number number
   * @return words
   */
  public static String words(long number) {
    if (number == 0) {
      return UNITS[0];
    }
    StringBuilder out = new StringBuilder();
    long rest = number;
    for (int scale = 0; rest > 0 && scale < SCALES.length; scale++) {
      int chunk = (int) (rest % THOUSAND);
      if (chunk > 0) {
        String part = belowThousand(chunk) + SCALES[scale];
        out.insert(0, out.isEmpty() ? part : part + " ");
      }
      rest /= THOUSAND;
    }
    return out.toString();
  }

  private static String belowThousand(int n) {
    StringBuilder sb = new StringBuilder();
    if (n >= HUNDRED) {
      sb.append(UNITS[n / HUNDRED]).append(" Hundred");
    }
    int rest = n % HUNDRED;
    if (rest > 0) {
      if (!sb.isEmpty()) {
        sb.append(' ');
      }
      sb.append(belowHundred(rest));
    }
    return sb.toString();
  }

  private static String belowHundred(int n) {
    if (n < TWENTY) {
      return UNITS[n];
    }
    String tens = TENS[n / TEN];
    return n % TEN == 0 ? tens : tens + "-" + UNITS[n % TEN];
  }
}
