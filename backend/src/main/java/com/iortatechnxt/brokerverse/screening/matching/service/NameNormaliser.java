package com.iortatechnxt.brokerverse.screening.matching.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Name normalisation of the matching engine (SNSRP-301, FR-SS-031): upper case without accents and
 * punctuation, titles and company forms dropped, and name particles joined to the following word so
 * that "Juan Dela Cruz", "Juan de la Cruz" and "JUAN DELA-CRUZ" give the same tokens {@code [JUAN,
 * DELACRUZ]}. Pure functions.
 */
public final class NameNormaliser {

  /** Accented Latin letters and their plain letter (same position in {@link #UNACCENTED}). */
  private static final String ACCENTED =
      "\u00c0\u00c1\u00c2\u00c3\u00c4\u00c5\u00c7\u00c8\u00c9\u00ca\u00cb\u00cc\u00cd\u00ce"
          + "\u00cf\u00d1\u00d2\u00d3\u00d4\u00d5\u00d6\u00d8\u00d9\u00da\u00db\u00dc\u00dd"
          + "\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u00e7\u00e8\u00e9\u00ea\u00eb\u00ec\u00ed"
          + "\u00ee\u00ef\u00f1\u00f2\u00f3\u00f4\u00f5\u00f6\u00f8\u00f9\u00fa\u00fb\u00fc"
          + "\u00fd\u00ff";

  private static final String UNACCENTED =
      "AAAAAACEEEEIIIINOOOOOOUUUUYaaaaaaceeeeiiiinoooooouuuuyy";
  private static final Pattern NON_ALNUM = Pattern.compile("[^A-Z0-9]+");

  /** Titles, generational suffixes and company forms that do not identify a name. */
  private static final Set<String> NOISE =
      Set.of(
          "MR",
          "MRS",
          "MS",
          "MISS",
          "DR",
          "ENGR",
          "ATTY",
          "HON",
          "SR",
          "JR",
          "II",
          "III",
          "IV",
          "INC",
          "CORP",
          "CORPORATION",
          "CO",
          "COMPANY",
          "LTD",
          "LIMITED",
          "LLC",
          "PLC",
          "THE",
          "INCORPORATED",
          "OPC");

  /** Particles joined to the next word ("DE LA CRUZ" becomes "DELACRUZ"). */
  private static final Set<String> PARTICLES =
      Set.of(
          "DE", "DEL", "DELA", "DELAS", "DELOS", "LA", "LAS", "LOS", "SAN", "SANTA", "STA", "STO",
          "VAN", "VON", "DI", "DA", "DOS", "DAS");

  private NameNormaliser() {}

  /**
   * The normalised tokens of a name, in their original order.
   *
   * @param name a name, may be {@code null}
   * @return the tokens, empty for a blank name
   */
  public static List<String> tokens(String name) {
    if (name == null || name.isBlank()) {
      return List.of();
    }
    String plain = plain(name);
    List<String> result = new ArrayList<>();
    StringBuilder particles = new StringBuilder();
    for (String word : plain.trim().split(" ")) {
      if (word.isEmpty() || NOISE.contains(word)) {
        continue;
      }
      if (PARTICLES.contains(word)) {
        particles.append(word);
      } else {
        result.add(particles.append(word).toString());
        particles.setLength(0);
      }
    }
    if (!particles.isEmpty()) {
      result.add(particles.toString());
    }
    return List.copyOf(result);
  }

  /**
   * A name in upper case without accents and with every other character than letters and digits
   * replaced by a space.
   *
   * @param name a name
   * @return the plain form
   */
  static String plain(String name) {
    return NON_ALNUM.matcher(withoutMarks(name).toUpperCase(Locale.ROOT)).replaceAll(" ");
  }

  private static String withoutMarks(String name) {
    StringBuilder b = new StringBuilder(name.length());
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      int at = ACCENTED.indexOf(c);
      b.append(at < 0 ? c : UNACCENTED.charAt(at));
    }
    return b.toString();
  }

  /**
   * The order-independent form of a name: its tokens sorted and joined with a space (the EXACT
   * key).
   *
   * @param tokens normalised tokens
   * @return the sorted form, empty for no tokens
   */
  public static String sorted(List<String> tokens) {
    return String.join(" ", tokens.stream().sorted().toList());
  }

  /**
   * Normalises an identifier (TIN, passport or ID number) to its letters and digits.
   *
   * @param id an identifier, may be {@code null}
   * @return upper-case letters and digits, empty when none
   */
  public static String identifier(String id) {
    return id == null ? "" : NON_ALNUM.matcher(id.toUpperCase(Locale.ROOT)).replaceAll("");
  }
}
