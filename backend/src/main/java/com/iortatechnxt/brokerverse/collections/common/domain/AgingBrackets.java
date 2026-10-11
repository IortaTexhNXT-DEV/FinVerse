package com.iortatechnxt.brokerverse.collections.common.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The aging brackets of the worklist, home chart and reports (parameter {@code CLX_AGING_BRACKETS},
 * OQ43): ranges of days such as {@code 0-30,31-45,46-60,61-90,91-120,121+}. The last range may be
 * open ({@code 121+} or {@code 121-}). A negative age (booking date in the future) falls in the
 * first bracket; an age between two ranges in the next one.
 */
public final class AgingBrackets {

  private static final Pattern RANGE = Pattern.compile("^(\\d{1,5})-(\\d{1,5})?\\+?$");
  private static final Pattern OPEN = Pattern.compile("^(\\d{1,5})\\+$");

  private final List<Bracket> brackets;

  private AgingBrackets(List<Bracket> brackets) {
    this.brackets = List.copyOf(brackets);
  }

  /**
   * Parses the brackets.
   *
   * @param items parameter items, e.g. {@code ["0-30", "31-45", "121+"]}
   * @return brackets, in order
   */
  public static AgingBrackets parse(List<String> items) {
    List<Bracket> parsed = new ArrayList<>();
    int previousTo = -1;
    for (String raw : items) {
      Bracket b = bracket(raw.strip().toUpperCase(Locale.ROOT));
      if (b.from() <= previousTo) {
        throw new BusinessRuleException(
            "CLX_AGING_BRACKETS_INVALID", "Aging brackets must be in ascending order: " + raw);
      }
      parsed.add(b);
      previousTo = b.to() == null ? Integer.MAX_VALUE : b.to();
    }
    if (parsed.isEmpty()) {
      parsed.add(new Bracket("0+", 0, null));
    }
    return new AgingBrackets(parsed);
  }

  private static Bracket bracket(String text) {
    Matcher open = OPEN.matcher(text);
    if (open.matches()) {
      return new Bracket(text, Integer.parseInt(open.group(1)), null);
    }
    Matcher range = RANGE.matcher(text);
    if (!range.matches()) {
      throw new BusinessRuleException(
          "CLX_AGING_BRACKETS_INVALID", "Aging bracket '" + text + "' is not from-to or from+");
    }
    int from = Integer.parseInt(range.group(1));
    if (range.group(2) == null) {
      return new Bracket(from + "+", from, null);
    }
    int to = Integer.parseInt(range.group(2));
    if (to < from) {
      throw new BusinessRuleException(
          "CLX_AGING_BRACKETS_INVALID", "Aging bracket '" + text + "' ends before it starts");
    }
    return new Bracket(text, from, to);
  }

  /**
   * The bracket of an age.
   *
   * @param days age in days
   * @return label of the bracket
   */
  public String labelOf(int days) {
    for (Bracket b : brackets) {
      if (b.to() == null || days <= b.to()) {
        return b.label();
      }
    }
    return brackets.get(brackets.size() - 1).label();
  }

  /**
   * Every bracket, in order.
   *
   * @return brackets
   */
  public List<Bracket> all() {
    return Collections.unmodifiableList(brackets);
  }

  /**
   * One aging bracket.
   *
   * @param label label, e.g. {@code 31-45}
   * @param from first day
   * @param to last day, null when open
   */
  public record Bracket(String label, int from, Integer to) {}
}
