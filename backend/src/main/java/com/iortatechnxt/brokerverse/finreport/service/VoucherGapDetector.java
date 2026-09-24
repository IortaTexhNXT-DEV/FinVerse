package com.iortatechnxt.brokerverse.finreport.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Finds gaps in voucher (journal batch) number series.
 *
 * <p>Voucher numbers look like {@code JV-HO-2026-000123}: the <b>series</b> is everything before
 * the last dash (transaction code, branch and year) and the <b>sequence</b> is the trailing number.
 * Gaps are computed within each series only, so moving from one year or branch series to another
 * never produces a bogus gap. Every number that exists (whatever the voucher status, including
 * cancelled or rejected vouchers) counts as present.
 */
public final class VoucherGapDetector {

  private static final char SEPARATOR = '-';

  private VoucherGapDetector() {}

  /**
   * Detects gaps: for consecutive existing sequence numbers a &lt; b with b &gt; a + 1, the numbers
   * a+1 .. b-1 are missing.
   *
   * @param voucherNumbers existing voucher numbers (any order, duplicates ignored)
   * @return gaps ordered by series and first missing number
   */
  public static List<VoucherGap> detect(Collection<String> voucherNumbers) {
    Map<String, SortedSet<Long>> series = new TreeMap<>();
    Map<String, Integer> widths = new TreeMap<>();
    for (String number : voucherNumbers) {
      int cut = number.lastIndexOf(SEPARATOR);
      String digits = number.substring(cut + 1);
      if (cut <= 0 || digits.isEmpty() || !digits.chars().allMatch(Character::isDigit)) {
        continue;
      }
      String key = number.substring(0, cut);
      series.computeIfAbsent(key, k -> new TreeSet<>()).add(Long.parseLong(digits));
      widths.merge(key, digits.length(), Math::max);
    }
    List<VoucherGap> gaps = new ArrayList<>();
    series.forEach((key, numbers) -> addGaps(gaps, key, widths.get(key), numbers));
    return gaps;
  }

  private static void addGaps(
      List<VoucherGap> gaps, String key, int width, SortedSet<Long> numbers) {
    Long previous = null;
    for (Long current : numbers) {
      if (previous != null && current > previous + 1) {
        gaps.add(
            new VoucherGap(
                key,
                format(key, previous + 1, width),
                format(key, current - 1, width),
                current - previous - 1));
      }
      previous = current;
    }
  }

  private static String format(String key, long sequence, int width) {
    StringBuilder digits = new StringBuilder(Long.toString(sequence));
    while (digits.length() < width) {
      digits.insert(0, '0');
    }
    return key + SEPARATOR + digits;
  }

  /**
   * A run of missing voucher numbers.
   *
   * @param series series key (transaction code, branch and year)
   * @param missingFrom first missing number
   * @param missingTo last missing number
   * @param count number of missing vouchers
   */
  public record VoucherGap(String series, String missingFrom, String missingTo, long count) {}
}
