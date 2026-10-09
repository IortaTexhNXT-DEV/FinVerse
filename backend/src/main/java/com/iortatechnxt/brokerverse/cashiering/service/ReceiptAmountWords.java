package com.iortatechnxt.brokerverse.cashiering.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * An amount in English words with the pesos and the centavos, as on the sample AR of BDOI
 * (FRS.CSH.02.06.02): 5,541,001.26 is "Five Million Five Hundred Forty One Thousand One Pesos and
 * Twenty Six Centavos". Another currency is written with its own unit names. The tens are written
 * without a hyphen, as on the client's form (the voucher words of the platform hyphenate them).
 */
public final class ReceiptAmountWords {

  private static final String[] ONES = {
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
  private static final String[] SCALES = {"", "Thousand", "Million", "Billion", "Trillion"};
  private static final int THOUSAND = 1000;
  private static final int HUNDRED = 100;
  private static final int TWENTY = 20;
  private static final int TEN = 10;

  private ReceiptAmountWords() {}

  /**
   * An amount in words.
   *
   * @param amount amount, rounded to the centavo
   * @param currency currency code (PHP is written in pesos and centavos)
   * @return words
   */
  public static String of(BigDecimal amount, String currency) {
    BigDecimal value = amount.abs().setScale(2, RoundingMode.HALF_UP);
    long whole = value.longValue();
    int cents = value.remainder(BigDecimal.ONE).movePointRight(2).intValue();
    String[] units = units(currency);
    StringBuilder text = new StringBuilder(whole(whole)).append(' ').append(units[0]);
    if (cents > 0) {
      text.append(" and ").append(below(cents)).append(' ').append(units[1]);
    }
    return amount.signum() < 0 ? "Minus " + text : text.toString();
  }

  private static String[] units(String currency) {
    if (currency == null || "PHP".equals(currency)) {
      return new String[] {"Pesos", "Centavos"};
    }
    if ("USD".equals(currency)) {
      return new String[] {"US Dollars", "Cents"};
    }
    return new String[] {currency, "Cents"};
  }

  /**
   * A whole number in words.
   *
   * @param number number
   * @return words
   */
  static String whole(long number) {
    if (number == 0) {
      return ONES[0];
    }
    List<String> groups = new ArrayList<>();
    long rest = number;
    int scale = 0;
    while (rest > 0) {
      int group = (int) (rest % THOUSAND);
      if (group > 0) {
        groups.add(0, below(group) + (scale > 0 ? " " + SCALES[scale] : ""));
      }
      rest /= THOUSAND;
      scale++;
    }
    return String.join(" ", groups);
  }

  private static String below(int number) {
    StringBuilder text = new StringBuilder();
    int rest = number;
    if (rest >= HUNDRED) {
      text.append(ONES[rest / HUNDRED]).append(" Hundred");
      rest %= HUNDRED;
    }
    if (rest >= TWENTY) {
      append(text, TENS[rest / TEN]);
      rest %= TEN;
    }
    if (rest > 0) {
      append(text, ONES[rest]);
    }
    return text.toString();
  }

  private static void append(StringBuilder text, String word) {
    if (!text.isEmpty()) {
      text.append(' ');
    }
    text.append(word);
  }
}
