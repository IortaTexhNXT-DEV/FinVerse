package com.iortatechnxt.brokerverse.report.core;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The parameter form of the Report Centre in business terms: a text parameter whose values come
 * from a platform list (users, insurers, sales units, product lines, cost centres, group profiles,
 * modules...) is offered as a choice from that list by name, and technical label suffixes such as
 * "(code)" or "(user ID)" are dropped. The report itself still receives the code.
 */
public final class ParameterLookups {

  /** Source of the users, by name. */
  public static final String USERS = PlatformCodeSets.USER;

  private static final Pattern TECHNICAL_SUFFIX =
      Pattern.compile("\\s*\\((?:code|area code|user ID|product line)\\)$");
  private static final Pattern CODE_WORD = Pattern.compile("\\s+(?:Code|code)$");

  /** Parameter names whose values come from a platform list, with the list. */
  private static final Map<String, String> SOURCES =
      Map.ofEntries(
          Map.entry("insurer", PlatformCodeSets.INSURER),
          Map.entry("insurerCode", PlatformCodeSets.INSURER),
          Map.entry("handler", USERS),
          Map.entry("accountOfficer", USERS),
          Map.entry("ao", USERS),
          Map.entry("officer", USERS),
          Map.entry("user", USERS),
          Map.entry("userId", USERS),
          Map.entry("username", USERS),
          Map.entry("inputter", USERS),
          Map.entry("authorizer", USERS),
          Map.entry("unitHead", USERS),
          Map.entry("agent", USERS),
          Map.entry("lineCode", PlatformCodeSets.PRODUCT_LINE),
          Map.entry("line", PlatformCodeSets.PRODUCT_LINE),
          Map.entry("riskType", PlatformCodeSets.PRODUCT_LINE),
          Map.entry("product", PlatformCodeSets.PRODUCT),
          Map.entry("salesTeam", PlatformCodeSets.SALES_UNIT),
          Map.entry("salesUnit", PlatformCodeSets.SALES_UNIT),
          Map.entry("marketingUnit", PlatformCodeSets.SALES_UNIT),
          Map.entry("team", PlatformCodeSets.SALES_UNIT),
          Map.entry("segment", PlatformCodeSets.SEGMENT),
          Map.entry("costCenter", PlatformCodeSets.COST_CENTRE),
          Map.entry("departmentFrom", PlatformCodeSets.COST_CENTRE),
          Map.entry("departmentTo", PlatformCodeSets.COST_CENTRE),
          Map.entry("bankFrom", PlatformCodeSets.BANK_ACCOUNT),
          Map.entry("bankTo", PlatformCodeSets.BANK_ACCOUNT),
          Map.entry("bankAccountCode", PlatformCodeSets.BANK_ACCOUNT),
          Map.entry("groupProfile", PlatformCodeSets.GROUP_PROFILE),
          Map.entry("area", PlatformCodeSets.MODULE),
          Map.entry("riskCategory", PlatformCodeSets.RISK_CATEGORY),
          Map.entry("schedule", PlatformCodeSets.GL_SCHEDULE),
          Map.entry("uploadNo", PlatformCodeSets.SOA_UPLOAD),
          Map.entry("bankAccount", PlatformCodeSets.BANK_ACCOUNT));

  /** Business labels of parameters whose declared label is technical. */
  private static final Map<String, String> LABELS =
      Map.of(
          "area", "Module",
          "userId", "User",
          "agent", "Agent",
          "insurerCode", "Insurer",
          "riskType", "Product Line",
          "schedule", "Schedule");

  /** A trailing reference to a requirement or legacy report ("(FGL008)", "(capability 4)"). */
  private static final Pattern REFERENCE =
      Pattern.compile("\\s*\\((?:[A-Z]{2,4}\\s?\\d[\\d.]*|capability \\d+)\\)$");

  /** A code in brackets written with underscores ("(due_to_bank)"). */
  private static final Pattern SNAKE = Pattern.compile("\\(([a-z_]{3,40})\\)");

  private ParameterLookups() {}

  /**
   * A report description in business words: without the trailing reference to the requirement or
   * the legacy report, and codes in brackets written as words.
   *
   * @param description declared description
   * @return description for the catalogue
   */
  public static String businessDescription(String description) {
    if (description == null) {
      return null;
    }
    String out = REFERENCE.matcher(description).replaceAll("");
    Matcher m = SNAKE.matcher(out);
    StringBuilder text = new StringBuilder();
    while (m.find()) {
      m.appendReplacement(text, Matcher.quoteReplacement("(" + m.group(1).replace('_', ' ') + ")"));
    }
    m.appendTail(text);
    return text.toString();
  }

  /**
   * The parameter as the form offers it: a list choice where the values come from a platform list,
   * with a business label.
   *
   * @param spec declared parameter
   * @return parameter for the form
   */
  public static ParameterSpec refine(ParameterSpec spec) {
    boolean bankAccount = spec.type() == ParameterType.ACCOUNT && "bankAccount".equals(spec.name());
    if (spec.type() != ParameterType.TEXT && !bankAccount) {
      return spec;
    }
    String label = LABELS.getOrDefault(spec.name(), businessLabel(spec.label()));
    String source = SOURCES.get(spec.name());
    if (source == null) {
      return label.equals(spec.label())
          ? spec
          : new ParameterSpec(
              spec.name(),
              label,
              spec.type(),
              spec.required(),
              spec.options(),
              spec.defaultValue());
    }
    return new ParameterSpec(
        spec.name(),
        label,
        ParameterType.LOOKUP,
        spec.required(),
        List.of(source),
        spec.defaultValue());
  }

  /** A label without its technical suffix: "Group Profile (code)" becomes "Group Profile". */
  static String businessLabel(String label) {
    String out = TECHNICAL_SUFFIX.matcher(label).replaceAll("");
    if (out.startsWith("Insurer")) {
      out = CODE_WORD.matcher(out).replaceAll("");
    }
    return out;
  }
}
