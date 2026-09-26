package com.iortatechnxt.brokerverse.screening.cases.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The AML Committee decision rule (SNSRP-704; FR-SS-064 R2, SQ15): parameter {@code
 * SCR_COMMITTEE_RULE} ANY (the first vote decides), MAJORITY (more than half of {@code
 * SCR_COMMITTEE_SIZE} members vote the same) or ALL (every member votes the same). When every
 * member has voted and no decision is reached, or when ALL can no longer be met, the case returns
 * to Compliance with the note "no majority". Pure function.
 */
public final class CommitteeRule {

  /** Decision when the rule cannot be met. */
  public static final String NO_MAJORITY = "NO_MAJORITY";

  private CommitteeRule() {}

  /**
   * Decides a round from its votes.
   *
   * @param rule ANY, MAJORITY or ALL (unknown values count as MAJORITY)
   * @param size committee size (at least 1)
   * @param votes the decisions so far, in order
   * @return the decision, {@value #NO_MAJORITY}, or empty while the round is open
   */
  public static Optional<String> decide(String rule, int size, List<String> votes) {
    if (votes.isEmpty()) {
      return Optional.empty();
    }
    int members = Math.max(1, size);
    String kind = rule == null ? "" : rule.strip().toUpperCase(Locale.ROOT);
    Map<String, Long> counts =
        votes.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    return switch (kind) {
      case "ANY" -> Optional.of(votes.get(0));
      case "ALL" -> all(members, votes, counts);
      default -> majority(members, votes, counts);
    };
  }

  private static Optional<String> majority(
      int members, List<String> votes, Map<String, Long> counts) {
    long needed = members / 2L + 1;
    Optional<String> reached =
        counts.entrySet().stream()
            .filter(e -> e.getValue() >= needed)
            .map(Map.Entry::getKey)
            .findFirst();
    if (reached.isPresent()) {
      return reached;
    }
    return votes.size() >= members ? Optional.of(NO_MAJORITY) : Optional.empty();
  }

  private static Optional<String> all(int members, List<String> votes, Map<String, Long> counts) {
    if (counts.size() > 1) {
      return Optional.of(NO_MAJORITY);
    }
    return votes.size() >= members ? Optional.of(votes.get(0)) : Optional.empty();
  }
}
