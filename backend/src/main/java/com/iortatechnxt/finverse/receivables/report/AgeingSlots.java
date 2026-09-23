package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Configurable ageing slots (rule R-AGE): up to five ascending day boundaries plus an "over"
 * bucket, e.g. {@code 30,60,90,120} gives 0-30, 31-60, 61-90, 91-120 and Over 120. Items not yet
 * due (negative age) fall into the first bucket.
 *
 * @param boundaries upper bounds of the buckets (inclusive)
 */
public record AgeingSlots(List<Integer> boundaries) {

  /** Default slots of the finance reports. */
  public static final String DEFAULT = "30,60,90,120";

  private static final int MAX_SLOTS = 5;
  private static final int MAX_DIGITS = 5;
  private static final Pattern SEPARATOR = Pattern.compile("[,;\\s]+");

  /** Canonical constructor copying the list. */
  public AgeingSlots {
    boundaries = List.copyOf(boundaries);
  }

  /**
   * Parses slot text such as "30,60,90,120".
   *
   * @param text boundaries separated by commas (blank = default)
   * @return slots
   */
  public static AgeingSlots parse(String text) {
    String value = text == null || text.isBlank() ? DEFAULT : text.trim();
    List<Integer> bounds = new ArrayList<>();
    for (String part : SEPARATOR.split(value)) {
      bounds.add(bound(part, bounds.isEmpty() ? 0 : bounds.get(bounds.size() - 1), value));
    }
    if (bounds.isEmpty() || bounds.size() > MAX_SLOTS) {
      throw invalid(value);
    }
    return new AgeingSlots(bounds);
  }

  /**
   * Number of buckets (boundaries plus the "over" bucket).
   *
   * @return count
   */
  public int size() {
    return boundaries.size() + 1;
  }

  /**
   * Bucket of an age in days.
   *
   * @param days age (negative = not yet due)
   * @return bucket index
   */
  public int index(long days) {
    for (int i = 0; i < boundaries.size(); i++) {
      if (days <= boundaries.get(i)) {
        return i;
      }
    }
    return boundaries.size();
  }

  /**
   * Column labels, e.g. "0-30", "31-60", "Over 120".
   *
   * @return labels in bucket order
   */
  public List<String> labels() {
    List<String> labels = new ArrayList<>();
    int from = 0;
    for (int bound : boundaries) {
      labels.add(from + "-" + bound);
      from = bound + 1;
    }
    labels.add("Over " + (from - 1));
    return labels;
  }

  private static int bound(String part, int previous, String value) {
    if (part.isEmpty()
        || part.length() > MAX_DIGITS
        || !part.chars().allMatch(Character::isDigit)) {
      throw invalid(value);
    }
    int bound = Integer.parseInt(part);
    if (bound <= previous) {
      throw invalid(value);
    }
    return bound;
  }

  private static BusinessRuleException invalid(String value) {
    return new BusinessRuleException(
        "INVALID_AGEING_SLOTS",
        "Ageing slots must be up to 5 ascending positive day limits, e.g. 30,60,90,120 (got "
            + value
            + ")");
  }
}
