package com.iortatechnxt.brokerverse.screening.matching.service;

import com.iortatechnxt.brokerverse.screening.config.domain.MatchAlgorithm;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Name similarity of the matching engine (SNSRP-301, FR-SS-031), 0 to 1, on normalised names. Pure
 * functions:
 *
 * <ul>
 *   <li><b>EXACT</b>: 1 when the sorted tokens are equal (word order, case, accents and punctuation
 *       ignored), otherwise 0;
 *   <li><b>PHONETIC</b>: the Dice coefficient of the tokens, two tokens agreeing when they share a
 *       Double Metaphone code ("Jon Santoz" and "John Santos" score 1);
 *   <li><b>FUZZY</b>: Jaro-Winkler of the whole sorted names or, for names with the same number of
 *       words, the average best token-to-token similarity if higher (typing errors such as "Juan
 *       Dela Crux").
 * </ul>
 */
public final class NameScorer {

  private static final double WINKLER_WEIGHT = 0.1;
  private static final int WINKLER_PREFIX = 4;
  private static final double JARO_THIRD = 3.0;

  private NameScorer() {}

  /**
   * Scores two names with an algorithm.
   *
   * @param algorithm the algorithm
   * @param a first name's keys
   * @param b second name's keys
   * @return the similarity, 0 to 1
   */
  public static double score(MatchAlgorithm algorithm, NameKeys a, NameKeys b) {
    if (a.isEmpty() || b.isEmpty()) {
      return 0;
    }
    return switch (algorithm) {
      case EXACT -> a.exact().equals(b.exact()) ? 1 : 0;
      case PHONETIC -> phonetic(a.tokens(), b.tokens());
      case FUZZY -> fuzzy(a, b);
    };
  }

  /**
   * Dice coefficient of two token lists, tokens agreeing by a shared Double Metaphone code; each
   * token of the second list is used once.
   *
   * @param a first tokens
   * @param b second tokens
   * @return 2 x agreeing tokens / (tokens of a + tokens of b)
   */
  static double phonetic(List<String> a, List<String> b) {
    List<Set<String>> pool = new ArrayList<>(b.stream().map(NameKeys::codes).toList());
    int agreeing = 0;
    for (String token : a) {
      Set<String> codes = NameKeys.codes(token);
      for (int i = 0; i < pool.size(); i++) {
        if (pool.get(i).stream().anyMatch(codes::contains)) {
          pool.remove(i);
          agreeing++;
          break;
        }
      }
    }
    return 2.0 * agreeing / (a.size() + b.size());
  }

  /**
   * Jaro-Winkler of the sorted names; for names with the same number of words also the average best
   * token-to-token similarity (a typing error in one word of a long name), whichever is higher.
   */
  private static double fuzzy(NameKeys a, NameKeys b) {
    double whole = jaroWinkler(a.exact(), b.exact());
    return a.tokens().size() == b.tokens().size() ? Math.max(whole, tokenAverage(a, b)) : whole;
  }

  private static double tokenAverage(NameKeys a, NameKeys b) {
    double sum = 0;
    for (String t : a.tokens()) {
      sum += best(t, b.tokens());
    }
    for (String t : b.tokens()) {
      sum += best(t, a.tokens());
    }
    return sum / (a.tokens().size() + b.tokens().size());
  }

  private static double best(String token, List<String> others) {
    return others.stream().mapToDouble(o -> jaroWinkler(token, o)).max().orElse(0);
  }

  /**
   * Jaro-Winkler similarity (prefix scale 0.1, prefix up to four characters).
   *
   * @param s first string
   * @param t second string
   * @return similarity, 0 to 1
   */
  public static double jaroWinkler(String s, String t) {
    if (s.equals(t)) {
      return 1;
    }
    double jaro = jaro(s, t);
    int prefix = 0;
    int limit = Math.min(WINKLER_PREFIX, Math.min(s.length(), t.length()));
    while (prefix < limit && s.charAt(prefix) == t.charAt(prefix)) {
      prefix++;
    }
    return jaro + prefix * WINKLER_WEIGHT * (1 - jaro);
  }

  private static double jaro(String s, String t) {
    if (s.isEmpty() || t.isEmpty()) {
      return 0;
    }
    int window = Math.max(0, Math.max(s.length(), t.length()) / 2 - 1);
    boolean[] sMatched = new boolean[s.length()];
    boolean[] tMatched = new boolean[t.length()];
    int matches = 0;
    for (int i = 0; i < s.length(); i++) {
      int end = Math.min(i + window + 1, t.length());
      for (int j = Math.max(0, i - window); j < end; j++) {
        if (!tMatched[j] && s.charAt(i) == t.charAt(j)) {
          sMatched[i] = true;
          tMatched[j] = true;
          matches++;
          break;
        }
      }
    }
    if (matches == 0) {
      return 0;
    }
    double halfTranspositions = transpositions(s, t, sMatched, tMatched) / 2.0;
    double m = matches;
    return (m / s.length() + m / t.length() + (m - halfTranspositions) / m) / JARO_THIRD;
  }

  private static int transpositions(String s, String t, boolean[] sMatched, boolean[] tMatched) {
    int k = 0;
    int count = 0;
    for (int i = 0; i < s.length(); i++) {
      if (!sMatched[i]) {
        continue;
      }
      while (!tMatched[k]) {
        k++;
      }
      if (s.charAt(i) != t.charAt(k)) {
        count++;
      }
      k++;
    }
    return count;
  }
}
