package com.iortatechnxt.brokerverse.screening.matching.service;

import java.text.Normalizer;
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

  private static final Pattern MARKS = Pattern.compile("\\p{M}+");
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
    String plain =
        MARKS
            .matcher(Normalizer.normalize(name, Normalizer.Form.NFD))
            .replaceAll("")
            .toUpperCase(Locale.ROOT);
    List<String> result = new ArrayList<>();
    StringBuilder particles = new StringBuilder();
    for (String word : NON_ALNUM.matcher(plain).replaceAll(" ").trim().split(" ")) {
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
