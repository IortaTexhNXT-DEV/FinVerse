package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.screening.config.domain.MatchAlgorithm;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchField;
import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * The name-matching criteria of one configuration version (SNSRP-101, FR-SS-011).
 *
 * @param version the version the rules belong to
 * @param rules the matching rules
 */
public record MatchCriteria(ConfigVersionRef version, List<Rule> rules) {

  /** Defensive copy. */
  public MatchCriteria {
    rules = List.copyOf(rules);
  }

  /**
   * The rules that apply to a list type and subject type.
   *
   * @param listType the list type code ({@code SCR_LIST_TYPE}, e.g. SANCTION)
   * @param subjectType the subject type
   * @return the rules, in their configured order
   */
  public List<Rule> rulesFor(String listType, SubjectType subjectType) {
    return rules.stream()
        .filter(r -> r.listType().equals(listType) && r.subjectType() == subjectType)
        .toList();
  }

  /**
   * One matching rule: for a list type and subject type, a pair scoring at or above {@code
   * threshold} with {@code algorithm} is a potential match; at or above {@code minScoreForCase} it
   * opens or joins a case (FR-SS-011 R2, R3).
   *
   * @param id the rule id ({@code scr_match_rule.id})
   * @param listType the list type code ({@code SCR_LIST_TYPE})
   * @param subjectType individual or entity
   * @param algorithm exact, phonetic or fuzzy
   * @param threshold the potential-match threshold, 0 to 1 (scale 4)
   * @param fields the fields compared; always contains {@link MatchField#NAME}
   * @param minScoreForCase the case threshold, 0 to 1, not below {@code threshold}
   */
  public record Rule(
      Long id,
      String listType,
      SubjectType subjectType,
      MatchAlgorithm algorithm,
      BigDecimal threshold,
      Set<MatchField> fields,
      BigDecimal minScoreForCase) {

    /** Defensive copy. */
    public Rule {
      fields = fields == null ? Set.of() : Set.copyOf(fields);
    }
  }
}
