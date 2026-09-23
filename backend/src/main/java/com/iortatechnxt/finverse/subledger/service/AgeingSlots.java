package com.iortatechnxt.finverse.subledger.service;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Configurable ageing slots (rule R-AGE of the finance reports spec) shared by the debtors and
 * creditors reports: up to five ascending upper bounds in days plus an "over" bucket, e.g. {@code
 * 30,60,90,120} gives 0-30, 31-60, 61-90, 91-120 and Over 120. Items not yet due (negative age)
 * fall into the first bucket.
 *
 * <p>The company-wide default comes from the {@code AGEING_BUCKETS} system parameter (see {@link
 * AgeingService#defaultSlots()}); reports let users override the slots per run.
 *
 * @param boundaries upper bounds of the buckets (inclusive), ascending and positive
 */
public record AgeingSlots(List<Integer> boundaries) {

  /** Built-in slots used when the system parameter is missing or unusable: 30/60/90/120. */
  public static final AgeingSlots STANDARD = new AgeingSlots(List.of(30, 60, 90, 120));

  /** Maximum number of slots (buckets before "over"). */
  public static final int MAX_SLOTS = 5;

  private static final int MAX_DIGITS = 5;
  private static final Pattern SEPARATOR = Pattern.compile("[,;\\s]+");

  /**
   * Validates and copies the bounds.
   *
   * @param boundaries 1 to 5 ascending positive upper bounds
   */
  public AgeingSlots {
    boundaries = List.copyOf(boundaries);
    if (boundaries.isEmpty() || boundaries.size() > MAX_SLOTS) {
      throw invalid(boundaries.toString());
    }
    int previous = 0;
    for (int bound : boundaries) {
      if (bound <= previous) {
        throw invalid(boundaries.toString());
      }
      previous = bound;
    }
  }

  /**
   * Creates slots from explicit bounds.
   *
   * @param boundaries 1 to 5 ascending positive upper bounds
   * @return slots
   */
  public static AgeingSlots of(List<Integer> boundaries) {
    return new AgeingSlots(boundaries);
  }

  /**
   * Parses slot text such as "30,60,90,120" (commas, semicolons or spaces separate the bounds).
   *
   * @param text slot text, blank for the fallback
   * @param fallback slots used when the text is blank
   * @return slots
   */
  public static AgeingSlots parse(String text, AgeingSlots fallback) {
    if (text == null || text.isBlank()) {
      return fallback;
    }
    String value = text.trim();
    List<Integer> bounds = new ArrayList<>();
    for (String part : SEPARATOR.split(value)) {
      if (part.isEmpty()
          || part.length() > MAX_DIGITS
          || !part.chars().allMatch(Character::isDigit)) {
        throw invalid(value);
      }
      bounds.add(Integer.parseInt(part));
    }
    if (bounds.size() > MAX_SLOTS) {
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
   * Heading of a bucket.
   *
   * @param index bucket
   * @return label, e.g. "31-60" or "Over 120"
   */
  public String label(int index) {
    if (index == boundaries.size()) {
      return "Over " + boundaries.get(index - 1);
    }
    int from = index == 0 ? 0 : boundaries.get(index - 1) + 1;
    return from + "-" + boundaries.get(index);
  }

  /**
   * Headings of every bucket, e.g. "0-30", "31-60", "Over 120".
   *
   * @return labels in bucket order
   */
  public List<String> labels() {
    List<String> labels = new ArrayList<>();
    for (int i = 0; i < size(); i++) {
      labels.add(label(i));
    }
    return labels;
  }

  /**
   * Echo text for report notes.
   *
   * @return e.g. "30/60/90/120"
   */
  public String describe() {
    return join("/");
  }

  /**
   * Text form accepted by {@link #parse}.
   *
   * @return e.g. "30,60,90,120"
   */
  public String text() {
    return join(",");
  }

  private String join(String separator) {
    return String.join(separator, boundaries.stream().map(String::valueOf).toList());
  }

  private static BusinessRuleException invalid(String value) {
    return new BusinessRuleException(
        "INVALID_AGEING_SLOTS",
        "Ageing slots must be up to 5 ascending positive day limits, e.g. 30,60,90,120 (got "
            + value
            + ")");
  }
}
