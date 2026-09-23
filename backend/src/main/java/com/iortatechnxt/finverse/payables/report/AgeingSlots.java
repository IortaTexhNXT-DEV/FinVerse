package com.iortatechnxt.finverse.payables.report;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Configurable ageing slots (rule R-AGE of the finance reports spec): up to five ascending upper
 * bounds in days plus an "over" bucket. Items not yet due (negative age) fall in the first slot.
 *
 * <p>Default 30/60/90/120 gives 0-30, 31-60, 61-90, 91-120 and Over 120.
 */
public final class AgeingSlots {

  /** Default creditor slots. */
  public static final List<Integer> DEFAULT = List.of(30, 60, 90, 120);

  /** Statement of payables slots (0-30, 31-60, 61-90, 91-180, 181-365, over 365). */
  public static final List<Integer> STATEMENT = List.of(30, 60, 90, 180, 365);

  private static final int MAX_SLOTS = 5;
  private static final String PARAM = "slot";

  private final List<Integer> bounds;

  private AgeingSlots(List<Integer> bounds) {
    this.bounds = List.copyOf(bounds);
  }

  /**
   * Creates slots from explicit bounds.
   *
   * @param bounds ascending positive upper bounds (1 to 5)
   * @return slots
   */
  public static AgeingSlots of(List<Integer> bounds) {
    if (bounds.isEmpty() || bounds.size() > MAX_SLOTS) {
      throw new BusinessRuleException("INVALID_AGEING_SLOTS", "Give between 1 and 5 ageing slots");
    }
    int previous = 0;
    for (int b : bounds) {
      if (b <= previous) {
        throw new BusinessRuleException(
            "INVALID_AGEING_SLOTS", "Ageing slots must be positive and ascending: " + bounds);
      }
      previous = b;
    }
    return new AgeingSlots(bounds);
  }

  /**
   * Reads slot parameters {@code slot1..slot5}; blanks are skipped, none given = defaults.
   *
   * @param p parameters
   * @param defaults defaults
   * @return slots
   */
  public static AgeingSlots from(ReportParameters p, List<Integer> defaults) {
    List<Integer> given = new ArrayList<>();
    for (int i = 1; i <= MAX_SLOTS; i++) {
      p.optionalDecimal(PARAM + i).map(BigDecimal::intValue).ifPresent(given::add);
    }
    return of(given.isEmpty() ? defaults : given);
  }

  /**
   * Parameter specs {@code slot1..slot5}.
   *
   * @return specs
   */
  public static List<ParameterSpec> parameters() {
    List<ParameterSpec> specs = new ArrayList<>();
    for (int i = 1; i <= MAX_SLOTS; i++) {
      specs.add(
          ParameterSpec.optional(PARAM + i, "Ageing Slot " + i + " (days)", ParameterType.NUMBER));
    }
    return specs;
  }

  /**
   * Number of buckets (slots + "over").
   *
   * @return count
   */
  public int size() {
    return bounds.size() + 1;
  }

  /**
   * Bucket of an age.
   *
   * @param days age in days (negative = not yet due)
   * @return bucket index
   */
  public int index(long days) {
    for (int i = 0; i < bounds.size(); i++) {
      if (days <= bounds.get(i)) {
        return i;
      }
    }
    return bounds.size();
  }

  /**
   * Cell key of a bucket.
   *
   * @param index bucket
   * @return key
   */
  public static String key(int index) {
    return "age" + index;
  }

  /**
   * Heading of a bucket.
   *
   * @param index bucket
   * @return label, e.g. "31-60" or "Over 120"
   */
  public String label(int index) {
    if (index == bounds.size()) {
      return "Over " + bounds.get(index - 1);
    }
    int from = index == 0 ? 0 : bounds.get(index - 1) + 1;
    return from + "-" + bounds.get(index);
  }

  /**
   * Amount columns of every bucket.
   *
   * @return columns
   */
  public List<ReportColumn> columns() {
    List<ReportColumn> cols = new ArrayList<>();
    for (int i = 0; i < size(); i++) {
      cols.add(ReportColumn.amount(key(i), label(i)));
    }
    return cols;
  }

  /**
   * Echo text.
   *
   * @return e.g. "30/60/90/120"
   */
  public String describe() {
    return String.join("/", bounds.stream().map(String::valueOf).toList());
  }
}
