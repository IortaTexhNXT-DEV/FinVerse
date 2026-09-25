package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Computes the difference between two contents of a configuration type as rule / attribute / before
 * / after lines (SNSRP-101, 108; FR-SS-010 R3). Pure functions: each row becomes an item keyed by
 * its identity in the type (e.g. list type, subject and algorithm of a matching rule) with its
 * attributes as text.
 */
public final class ConfigDiff {

  private static final int SCORE_SCALE = 4;
  private static final String CASE_TYPE = "Case type";
  private static final String RISK_CATEGORY = "Risk category";
  private static final String ORDER = "Order";
  private static final String ANY = "*";

  private ConfigDiff() {}

  /**
   * The changes from {@code before} to {@code after}.
   *
   * @param type the configuration type
   * @param before the content in force (empty when none)
   * @param after the draft content
   * @return the changed attributes, added rows and removed rows, in item order
   */
  public static List<ConfigChange> between(
      ConfigType type, ConfigContent before, ConfigContent after) {
    Map<String, Map<String, String>> old = lines(type, before);
    Map<String, Map<String, String>> now = lines(type, after);
    Set<String> items = new LinkedHashSet<>(old.keySet());
    items.addAll(now.keySet());
    List<ConfigChange> changes = new ArrayList<>();
    for (String item : items) {
      Map<String, String> a = old.getOrDefault(item, Map.of());
      Map<String, String> b = now.getOrDefault(item, Map.of());
      Set<String> attributes = new LinkedHashSet<>(a.keySet());
      attributes.addAll(b.keySet());
      for (String attribute : attributes) {
        String x = a.get(attribute);
        String y = b.get(attribute);
        if (!Objects.equals(x, y)) {
          changes.add(new ConfigChange(item, attribute, x, y));
        }
      }
    }
    return changes;
  }

  /**
   * The content of a type as items and attributes.
   *
   * @param type type
   * @param content content
   * @return items in order
   */
  static Map<String, Map<String, String>> lines(ConfigType type, ConfigContent content) {
    Map<String, Map<String, String>> out = new LinkedHashMap<>();
    switch (type) {
      case MATCH_CRITERIA -> content.matchRules().forEach(r -> match(out, r));
      case RISK_RULES -> {
        content.riskCategories().forEach(c -> category(out, c));
        content.riskRules().forEach(r -> riskRule(out, r));
      }
      case APPROVAL_MATRIX -> content.routes().forEach(r -> route(out, r));
      case ASSIGNMENT_MATRIX -> content.assignmentRules().forEach(r -> assignment(out, r));
      case SLA_MATRIX -> content.slaRules().forEach(r -> sla(out, r));
      case VALIDATION_RULES -> content.validationRules().forEach(r -> validation(out, r));
      case TEMPLATE -> template(out, content.template());
      case STR_LAYOUT -> layout(out, content.layout());
    }
    return out;
  }

  private static void match(Map<String, Map<String, String>> out, MatchCriteria.Rule r) {
    Map<String, String> a = item(out, r.listType() + " " + r.subjectType() + " " + r.algorithm());
    a.put("Threshold", score(r.threshold()));
    a.put("Fields compared", codes(r.fields().stream().map(Enum::name).toList()));
    a.put("Case threshold", score(r.minScoreForCase()));
  }

  private static void category(Map<String, Map<String, String>> out, RiskRules.Category c) {
    Map<String, String> a = item(out, "Category " + c.code());
    a.put("Name", c.name());
    a.put("Tier", String.valueOf(c.tier()));
    a.put("Risk rating", c.kycRiskRating());
    a.put("Tags", codes(c.tags()));
    a.put(CASE_TYPE, text(c.caseType()));
    a.put("Requires EDD", String.valueOf(c.requiresEdd()));
  }

  private static void riskRule(Map<String, Map<String, String>> out, RiskRules.Rule r) {
    Map<String, String> a = item(out, "Rule " + r.priority());
    a.put("Category", r.categoryCode());
    a.put("Attribute", String.valueOf(r.attribute()));
    a.put("Operator", String.valueOf(r.operator()));
    a.put("Values", codes(r.values()));
  }

  private static void route(Map<String, Map<String, String>> out, ApprovalMatrix.Route r) {
    Map<String, String> a = item(out, "Route " + r.order());
    a.put("From stage", r.fromStage());
    a.put(CASE_TYPE, text(r.caseType()));
    a.put(RISK_CATEGORY, text(r.riskCategory()));
    a.put("Marketing unit", text(r.marketingUnit()));
    a.put("Disposition", text(r.disposition()));
    a.put("To stage", r.toStage());
    a.put(
        "Approver",
        text(r.approverKind() == null ? null : r.approverKind().name())
            + (r.approverValue() == null ? "" : " " + r.approverValue()));
  }

  private static void assignment(Map<String, Map<String, String>> out, AssignmentMatrix.Rule r) {
    Map<String, String> a = item(out, "Scenario " + r.order());
    a.put(CASE_TYPE, text(r.caseType()));
    a.put("Trigger", text(r.trigger()));
    a.put(RISK_CATEGORY, text(r.riskCategory()));
    a.put("Marketing unit", text(r.marketingUnit()));
    a.put("Client type", text(r.clientType()));
    a.put("Team role", text(r.teamRole()));
    a.put("User", text(r.user()));
    a.put("Balancing", String.valueOf(r.balancing()));
  }

  private static void sla(Map<String, Map<String, String>> out, SlaMatrix.Rule r) {
    Map<String, String> a =
        item(out, "SLA " + r.stage() + " / " + text(r.caseType()) + " / " + text(r.riskCategory()));
    a.put("SLA hours", String.valueOf(r.slaHours()));
    a.put("Reminder lead hours", String.valueOf(r.reminderLeadHours()));
    a.put("Escalate to", r.escalateToRole());
    a.put("Calendar", String.valueOf(r.calendar()));
  }

  private static void validation(Map<String, Map<String, String>> out, ValidationRules.Rule r) {
    Map<String, String> a =
        item(out, "Check " + r.rule() + " / " + text(r.stage()) + " / " + text(r.caseType()));
    a.put("Parameters", text(r.parameters()));
    a.put("Blocking", String.valueOf(r.blocking()));
  }

  private static void template(Map<String, Map<String, String>> out, ConfigContent.Template t) {
    if (t == null) {
      return;
    }
    item(out, "Template").put("Name", t.name());
    for (ReviewTemplate.Field f : t.fields()) {
      Map<String, String> a = item(out, "Field " + f.code());
      a.put("Section", f.section());
      a.put("Label", f.label());
      a.put("Data type", String.valueOf(f.dataType()));
      a.put("List type", text(f.lovType()));
      a.put("Mandatory", String.valueOf(f.mandatory()));
      a.put("Help", text(f.help()));
      a.put(ORDER, String.valueOf(f.order()));
      a.put("Prefill", text(f.prefillSource()));
    }
  }

  private static void layout(Map<String, Map<String, String>> out, ConfigContent.Layout l) {
    if (l == null) {
      return;
    }
    Map<String, String> h = item(out, "Layout");
    h.put("Format", String.valueOf(l.format()));
    h.put("Delimiter", text(l.delimiter()));
    h.put("Encoding", text(l.encoding()));
    for (StrLayout.Column c : l.columns()) {
      Map<String, String> a = item(out, "Column " + c.order());
      a.put("STR field", text(c.fieldCode()));
      a.put("Fixed value", text(c.fixedValue()));
      a.put("Header", text(c.header()));
      a.put("Length", c.length() == null ? "" : String.valueOf(c.length()));
      a.put("Padding", text(c.pad()));
      a.put(
          "Code map",
          new TreeSet<>(
                  c.codeMap().entrySet().stream()
                      .map(e -> e.getKey() + "=" + e.getValue())
                      .toList())
              .toString());
    }
  }

  private static Map<String, String> item(Map<String, Map<String, String>> out, String key) {
    return out.computeIfAbsent(key, k -> new LinkedHashMap<>());
  }

  private static String score(BigDecimal value) {
    return value == null ? "" : value.setScale(SCORE_SCALE, RoundingMode.HALF_UP).toPlainString();
  }

  private static String codes(Collection<String> codes) {
    return String.join(", ", new TreeSet<>(codes));
  }

  private static String text(String value) {
    return value == null || value.isBlank() ? ANY : value;
  }
}
