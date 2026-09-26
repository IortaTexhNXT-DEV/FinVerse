package com.iortatechnxt.brokerverse.screening.matching.service;

import com.iortatechnxt.brokerverse.screening.config.domain.MatchField;
import com.iortatechnxt.brokerverse.screening.config.service.MatchCriteria;
import com.iortatechnxt.brokerverse.screening.watchlist.service.ListedEntry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Scores one client against one watchlist entry with the matching rules of the entry's list type
 * and subject type (SNSRP-301, FR-SS-031). Pure functions.
 *
 * <p>For each rule the best name score of the rule's algorithm is taken over the client's names and
 * the entry's primary name (field NAME) and, when the rule compares ALIAS, its aliases. Where the
 * rule compares them and both sides hold them, the birth date, nationality and ID numbers raise or
 * lower the score (FR-SS-031: "raise or lower the score"). A rule hits when the score reaches its
 * threshold; the best hit wins, ties going to the earlier rule (EXACT before PHONETIC before FUZZY
 * in the configured order). The case threshold of the winning rule tells whether the match opens a
 * case (FR-SS-034).
 */
public final class PairMatcher {

  /** Score added when the birth dates are equal. */
  static final double BIRTH_DATE_BONUS = 0.05;

  /** Score removed when both birth dates are known and differ. */
  static final double BIRTH_DATE_PENALTY = 0.10;

  /** Score added when the nationalities are equal. */
  static final double NATIONALITY_BONUS = 0.02;

  /** Score removed when both nationalities are known and differ. */
  static final double NATIONALITY_PENALTY = 0.05;

  /** Score added when an ID number is shared. */
  static final double ID_BONUS = 0.10;

  private static final int SCALE = 4;
  private static final int MIN_ID = 4;

  private PairMatcher() {}

  /**
   * The best hit of a client against an entry.
   *
   * @param subject the client
   * @param entry the entry
   * @param criteria the matching criteria
   * @return the hit, empty when no rule reaches its threshold
   */
  public static Optional<Hit> match(
      ScreeningSubject subject, ListedEntry entry, MatchCriteria criteria) {
    if (subject.subjectType() != entry.entityType()) {
      return Optional.empty();
    }
    List<NameKeys> clientNames = subject.keys();
    List<NameKeys> entryNames = names(entry);
    List<NameKeys> aliasNames = entry.aliases().stream().map(NameKeys::of).toList();
    Hit best = null;
    for (MatchCriteria.Rule rule : criteria.rulesFor(entry.listType(), entry.entityType())) {
      Hit hit = score(subject, entry, rule, clientNames, entryNames, aliasNames);
      if (hit != null && (best == null || hit.score().compareTo(best.score()) > 0)) {
        best = hit;
      }
    }
    return Optional.ofNullable(best);
  }

  private static Hit score(
      ScreeningSubject subject,
      ListedEntry entry,
      MatchCriteria.Rule rule,
      List<NameKeys> clientNames,
      List<NameKeys> entryNames,
      List<NameKeys> aliasNames) {
    Set<MatchField> fields = EnumSet.noneOf(MatchField.class);
    double name = best(rule, clientNames, entryNames);
    double alias =
        rule.fields().contains(MatchField.ALIAS) ? best(rule, clientNames, aliasNames) : 0;
    if (name <= 0 && alias <= 0) {
      return null;
    }
    fields.add(name >= alias ? MatchField.NAME : MatchField.ALIAS);
    double score = Math.max(name, alias) + adjustments(subject, entry, rule, fields);
    BigDecimal rounded =
        BigDecimal.valueOf(Math.min(1, Math.max(0, score))).setScale(SCALE, RoundingMode.HALF_UP);
    if (rounded.compareTo(rule.threshold()) < 0) {
      return null;
    }
    boolean reachesCase =
        rule.minScoreForCase() != null && rounded.compareTo(rule.minScoreForCase()) >= 0;
    return new Hit(entry, rule, rounded, Set.copyOf(fields), reachesCase);
  }

  private static double adjustments(
      ScreeningSubject subject,
      ListedEntry entry,
      MatchCriteria.Rule rule,
      Set<MatchField> fields) {
    double delta = 0;
    if (rule.fields().contains(MatchField.BIRTH_DATE)
        && subject.birthDate() != null
        && entry.birthDate() != null) {
      boolean same = subject.birthDate().equals(entry.birthDate());
      delta += same ? BIRTH_DATE_BONUS : -BIRTH_DATE_PENALTY;
      addIf(fields, same, MatchField.BIRTH_DATE);
    }
    if (rule.fields().contains(MatchField.NATIONALITY)
        && present(subject.nationality())
        && present(entry.nationality())) {
      boolean same = normal(subject.nationality()).equals(normal(entry.nationality()));
      delta += same ? NATIONALITY_BONUS : -NATIONALITY_PENALTY;
      addIf(fields, same, MatchField.NATIONALITY);
    }
    if (rule.fields().contains(MatchField.ID) && sharesId(subject, entry)) {
      delta += ID_BONUS;
      fields.add(MatchField.ID);
    }
    return delta;
  }

  private static void addIf(Set<MatchField> fields, boolean condition, MatchField field) {
    if (condition) {
      fields.add(field);
    }
  }

  private static boolean sharesId(ScreeningSubject subject, ListedEntry entry) {
    if (entry.idNumbers() == null || subject.ids().isEmpty()) {
      return false;
    }
    return Stream.of(entry.idNumbers().split("[;,]"))
        .map(NameNormaliser::identifier)
        .filter(id -> id.length() >= MIN_ID)
        .anyMatch(subject.ids()::contains);
  }

  private static double best(MatchCriteria.Rule rule, List<NameKeys> a, List<NameKeys> b) {
    double best = 0;
    for (NameKeys x : a) {
      for (NameKeys y : b) {
        best = Math.max(best, NameScorer.score(rule.algorithm(), x, y));
      }
    }
    return best;
  }

  /**
   * The keys of an entry's own names (primary name, and first and last name when given).
   *
   * @param entry the entry
   * @return keys per name
   */
  static List<NameKeys> names(ListedEntry entry) {
    Stream<String> parts =
        present(entry.firstName()) && present(entry.lastName())
            ? Stream.of(
                entry.primaryName(), entry.firstName().trim() + " " + entry.lastName().trim())
            : Stream.of(entry.primaryName());
    return parts.map(NameKeys::of).filter(k -> !k.isEmpty()).distinct().toList();
  }

  private static boolean present(String value) {
    return value != null && !value.isBlank();
  }

  private static String normal(String value) {
    return value.trim().toUpperCase(Locale.ROOT);
  }

  /**
   * A rule that reached its threshold for a client and an entry.
   *
   * @param entry the entry
   * @param rule the matching rule
   * @param score the score, 0 to 1 (scale 4)
   * @param fields the fields that matched
   * @param reachesCase whether the score reaches the rule's case threshold
   */
  public record Hit(
      ListedEntry entry,
      MatchCriteria.Rule rule,
      BigDecimal score,
      Set<MatchField> fields,
      boolean reachesCase) {

    /** Defensive copy. */
    public Hit {
      fields = Set.copyOf(fields);
    }
  }
}
