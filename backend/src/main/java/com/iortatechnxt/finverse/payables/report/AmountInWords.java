package com.iortatechnxt.finverse.payables.report;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Spells an amount for vouchers and cheques, e.g. {@code PHP ONE THOUSAND TWO HUNDRED FIFTY AND
 * 75/100 ONLY}.
 */
public final class AmountInWords {

  private static final String[] ONES = {
    "",
    "ONE",
    "TWO",
    "THREE",
    "FOUR",
    "FIVE",
    "SIX",
    "SEVEN",
    "EIGHT",
    "NINE",
    "TEN",
    "ELEVEN",
    "TWELVE",
    "THIRTEEN",
    "FOURTEEN",
    "FIFTEEN",
    "SIXTEEN",
    "SEVENTEEN",
    "EIGHTEEN",
    "NINETEEN"
  };
  private static final String[] TENS = {
    "", "", "TWENTY", "THIRTY", "FORTY", "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY"
  };
  private static final String[] SCALES = {"", " THOUSAND", " MILLION", " BILLION", " TRILLION"};
  private static final int THOUSAND = 1000;
  private static final int HUNDRED = 100;
  private static final int TWENTY = 20;

  private AmountInWords() {}

  /**
   * Spells an amount.
   *
   * @param currency currency code prefix
   * @param amount non-negative amount
   * @return words
   */
  public static String of(String currency, BigDecimal amount) {
    BigDecimal value = amount.abs().setScale(2, RoundingMode.HALF_EVEN);
    long whole = value.longValue();
    int cents = value.remainder(BigDecimal.ONE).movePointRight(2).intValue();
    String words = whole == 0 ? "ZERO" : spell(whole);
    return currency + " " + words + " AND " + String.format("%02d", cents) + "/100 ONLY";
  }

  private static String spell(long number) {
    StringBuilder out = new StringBuilder();
    long rest = number;
    int scale = 0;
    while (rest > 0) {
      int group = (int) (rest % THOUSAND);
      if (group > 0) {
        String text = hundreds(group) + SCALES[scale];
        out.insert(0, out.isEmpty() ? text : text + " ");
      }
      rest /= THOUSAND;
      scale++;
    }
    return out.toString();
  }

  private static String hundreds(int n) {
    StringBuilder out = new StringBuilder();
    if (n >= HUNDRED) {
      out.append(ONES[n / HUNDRED]).append(" HUNDRED");
    }
    int rest = n % HUNDRED;
    if (rest > 0) {
      if (!out.isEmpty()) {
        out.append(' ');
      }
      out.append(rest < TWENTY ? ONES[rest] : tens(rest));
    }
    return out.toString();
  }

  private static String tens(int n) {
    return n % 10 == 0 ? TENS[n / 10] : TENS[n / 10] + "-" + ONES[n % 10];
  }
}
