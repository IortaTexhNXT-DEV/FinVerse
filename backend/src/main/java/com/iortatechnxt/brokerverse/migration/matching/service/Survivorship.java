package com.iortatechnxt.brokerverse.migration.matching.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.migration.mapping.domain.SurvivorshipRule;
import com.iortatechnxt.brokerverse.migration.mapping.domain.SurvivorshipRuleRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Survivorship of a merged client (DATA_MIGRATION_DESIGN section 9): per field the first non-blank
 * value by source priority (default QPS, then EBIX, then CMS), or the most recently updated value
 * where the rule says LATEST. Every value that lost is kept for review on the match pairs.
 */
@Component
public class Survivorship {

  /** Default source priority. */
  public static final List<String> DEFAULT_PRIORITY =
      List.of("QPS", "EBIX", "CMS", "EXCEL", "ISYS");

  private static final String UPDATED = "last_updated";

  private final SurvivorshipRuleRepository rules;
  private final ObjectMapper json;

  /**
   * Creates the component.
   *
   * @param rules survivorship rules
   * @param json JSON mapper
   */
  public Survivorship(SurvivorshipRuleRepository rules, ObjectMapper json) {
    this.rules = rules;
    this.json = json;
  }

  /**
   * Merges the members of a cluster.
   *
   * @param members members with their source and values
   * @return survivor, merged values and the values that lost
   */
  @Transactional(readOnly = true)
  public Result merge(List<Member> members) {
    Map<String, SurvivorshipRule> byField = new HashMap<>();
    rules.findAll().forEach(r -> byField.put(r.getField(), r));
    List<Member> ordered = new ArrayList<>(members);
    ordered.sort(Comparator.comparingInt(m -> rank(DEFAULT_PRIORITY, m.source())));
    Member survivor = ordered.get(0);
    Map<String, String> merged = new LinkedHashMap<>();
    Map<String, List<String>> lost = new LinkedHashMap<>();
    SortedSet<String> fields = new TreeSet<>();
    members.forEach(m -> fields.addAll(m.values().keySet()));
    for (String field : fields) {
      SurvivorshipRule rule = byField.get(field);
      List<Member> order = orderFor(rule, members);
      String value = null;
      for (Member m : order) {
        String v = m.values().get(field);
        if (v == null || v.isBlank()) {
          continue;
        }
        if (value == null) {
          value = v;
        } else if (!value.equals(v)) {
          lost.computeIfAbsent(field, k -> new ArrayList<>()).add(m.key() + "=" + v);
        }
      }
      if (value != null) {
        merged.put(field, value);
      }
    }
    return new Result(survivor.key(), merged, toJson(lost));
  }

  private static List<Member> orderFor(SurvivorshipRule rule, List<Member> members) {
    List<Member> order = new ArrayList<>(members);
    if (rule != null && rule.latest()) {
      order.sort(
          Comparator.comparing((Member m) -> m.values().getOrDefault(UPDATED, "")).reversed());
    } else {
      List<String> priority = rule == null ? DEFAULT_PRIORITY : rule.priority();
      order.sort(Comparator.comparingInt(m -> rank(priority, m.source())));
    }
    return order;
  }

  private static int rank(List<String> priority, String source) {
    int i = priority.indexOf(source);
    return i < 0 ? priority.size() : i;
  }

  private String toJson(Map<String, List<String>> lost) {
    if (lost.isEmpty()) {
      return null;
    }
    try {
      return json.writeValueAsString(lost);
    } catch (JsonProcessingException e) {
      return lost.toString();
    }
  }

  /**
   * A member of a cluster.
   *
   * @param key source and legacy key
   * @param source source system
   * @param values mapped values
   */
  public record Member(String key, String source, Map<String, String> values) {}

  /**
   * The merged client.
   *
   * @param survivorKey key of the surviving record
   * @param values merged values
   * @param lostValuesJson values that lost, by field (JSON), null when none
   */
  public record Result(String survivorKey, Map<String, String> values, String lostValuesJson) {}
}
