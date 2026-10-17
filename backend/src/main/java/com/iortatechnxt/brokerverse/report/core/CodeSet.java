package com.iortatechnxt.brokerverse.report.core;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An include or exclude list of codes: "only these" or "all except these" (Renewal filter criteria
 * and report parameters, BRD x.009.2 and BRRN.003). An empty list selects everything.
 *
 * @param values the codes, never null
 * @param exclude true for "all except", false for "only"
 */
public record CodeSet(Set<String> values, boolean exclude) {

  /** Everything (no selection). */
  public static final CodeSet ALL = new CodeSet(Set.of(), false);

  private static final String EXCEPT = "!";

  /** Defensive copy. */
  public CodeSet {
    values = values == null ? Set.of() : Set.copyOf(values);
  }

  /**
   * Reads the wire form: a comma separated list, prefixed with {@code !} for "all except".
   *
   * @param text value, may be blank
   * @return the selection
   */
  public static CodeSet parse(String text) {
    if (text == null || text.isBlank()) {
      return ALL;
    }
    String trimmed = text.strip();
    boolean except = trimmed.startsWith(EXCEPT);
    String list = except ? trimmed.substring(1) : trimmed;
    Set<String> codes =
        Arrays.stream(list.split(","))
            .map(String::strip)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    return codes.isEmpty() ? ALL : new CodeSet(codes, except);
  }

  /**
   * An "only these" selection.
   *
   * @param codes codes
   * @return selection
   */
  public static CodeSet only(List<String> codes) {
    return new CodeSet(Set.copyOf(codes), false);
  }

  /**
   * Whether nothing is selected.
   *
   * @return true when every value passes
   */
  public boolean isAll() {
    return values.isEmpty();
  }

  /**
   * Whether a value passes the selection.
   *
   * @param value value, may be null (passes only "all" and "all except")
   * @return true when selected
   */
  public boolean matches(String value) {
    if (isAll()) {
      return true;
    }
    boolean listed = value != null && values.contains(value);
    return exclude != listed;
  }

  /**
   * The codes as an array for a SQL {@code any(?)} parameter; a placeholder when empty so the array
   * is never empty.
   *
   * @return codes
   */
  public String[] array() {
    return values.isEmpty() ? new String[] {""} : values.toArray(String[]::new);
  }

  /**
   * The wire form of the selection.
   *
   * @return text, empty for "all"
   */
  public String text() {
    if (isAll()) {
      return "";
    }
    String list = values.stream().sorted().collect(Collectors.joining(","));
    return exclude ? EXCEPT + list : list;
  }
}
