package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.domain.StrFormat;
import com.iortatechnxt.brokerverse.screening.config.domain.TemplateType;
import java.util.List;

/**
 * The editable content of a configuration version (SNSRP-101-108): the rows of its type. Only the
 * lists of the version's type are used; the others are empty. It is the draft editor's payload and
 * the input of the difference against the version in force.
 *
 * @param matchRules MATCH_CRITERIA rows
 * @param riskCategories RISK_RULES categories
 * @param riskRules RISK_RULES rules
 * @param routes APPROVAL_MATRIX routes
 * @param assignmentRules ASSIGNMENT_MATRIX scenarios
 * @param slaRules SLA_MATRIX rows
 * @param validationRules VALIDATION_RULES rows
 * @param template TEMPLATE header and fields, {@code null} for the other types
 * @param layout STR_LAYOUT header and columns, {@code null} for the other types
 */
public record ConfigContent(
    List<MatchCriteria.Rule> matchRules,
    List<RiskRules.Category> riskCategories,
    List<RiskRules.Rule> riskRules,
    List<ApprovalMatrix.Route> routes,
    List<AssignmentMatrix.Rule> assignmentRules,
    List<SlaMatrix.Rule> slaRules,
    List<ValidationRules.Rule> validationRules,
    Template template,
    Layout layout) {

  /** Null lists become empty. */
  public ConfigContent {
    matchRules = copy(matchRules);
    riskCategories = copy(riskCategories);
    riskRules = copy(riskRules);
    routes = copy(routes);
    assignmentRules = copy(assignmentRules);
    slaRules = copy(slaRules);
    validationRules = copy(validationRules);
  }

  private static <T> List<T> copy(List<T> list) {
    return list == null ? List.of() : List.copyOf(list);
  }

  /**
   * Empty content.
   *
   * @return content without rows
   */
  public static ConfigContent empty() {
    return new ConfigContent(null, null, null, null, null, null, null, null, null);
  }

  /**
   * MATCH_CRITERIA content.
   *
   * @param rules matching rules
   * @return content
   */
  public static ConfigContent ofMatch(List<MatchCriteria.Rule> rules) {
    return new ConfigContent(rules, null, null, null, null, null, null, null, null);
  }

  /**
   * RISK_RULES content.
   *
   * @param categories categories
   * @param rules rules
   * @return content
   */
  public static ConfigContent ofRisk(
      List<RiskRules.Category> categories, List<RiskRules.Rule> rules) {
    return new ConfigContent(null, categories, rules, null, null, null, null, null, null);
  }

  /**
   * APPROVAL_MATRIX content.
   *
   * @param routes routes
   * @return content
   */
  public static ConfigContent ofRoutes(List<ApprovalMatrix.Route> routes) {
    return new ConfigContent(null, null, null, routes, null, null, null, null, null);
  }

  /**
   * ASSIGNMENT_MATRIX content.
   *
   * @param rules scenarios
   * @return content
   */
  public static ConfigContent ofAssignments(List<AssignmentMatrix.Rule> rules) {
    return new ConfigContent(null, null, null, null, rules, null, null, null, null);
  }

  /**
   * SLA_MATRIX content.
   *
   * @param rules SLA rows
   * @return content
   */
  public static ConfigContent ofSla(List<SlaMatrix.Rule> rules) {
    return new ConfigContent(null, null, null, null, null, rules, null, null, null);
  }

  /**
   * VALIDATION_RULES content.
   *
   * @param rules rules
   * @return content
   */
  public static ConfigContent ofValidation(List<ValidationRules.Rule> rules) {
    return new ConfigContent(null, null, null, null, null, null, rules, null, null);
  }

  /**
   * TEMPLATE content.
   *
   * @param template template, may be {@code null}
   * @return content
   */
  public static ConfigContent ofTemplate(Template template) {
    return new ConfigContent(null, null, null, null, null, null, null, template, null);
  }

  /**
   * STR_LAYOUT content.
   *
   * @param layout layout, may be {@code null}
   * @return content
   */
  public static ConfigContent ofLayout(Layout layout) {
    return new ConfigContent(null, null, null, null, null, null, null, null, layout);
  }

  /**
   * Whether the content has no rule for a type ("Add at least one rule before submitting").
   *
   * @param type the version's type
   * @return true when empty
   */
  public boolean isEmptyFor(ConfigType type) {
    return switch (type) {
      case MATCH_CRITERIA -> matchRules.isEmpty();
      case RISK_RULES -> riskCategories.isEmpty() || riskRules.isEmpty();
      case APPROVAL_MATRIX -> routes.isEmpty();
      case ASSIGNMENT_MATRIX -> assignmentRules.isEmpty();
      case SLA_MATRIX -> slaRules.isEmpty();
      case VALIDATION_RULES -> validationRules.isEmpty();
      case TEMPLATE -> template == null || template.fields().isEmpty();
      case STR_LAYOUT -> layout == null || layout.columns().isEmpty();
    };
  }

  /**
   * Template header and fields.
   *
   * @param templateType the template type (= the version scope)
   * @param name display name
   * @param fields fields
   */
  public record Template(
      TemplateType templateType, String name, List<ReviewTemplate.Field> fields) {

    /** Null list becomes empty. */
    public Template {
      fields = fields == null ? List.of() : List.copyOf(fields);
    }
  }

  /**
   * STR layout header and columns.
   *
   * @param format file format
   * @param delimiter delimiter, may be {@code null}
   * @param encoding encoding
   * @param columns columns
   */
  public record Layout(
      StrFormat format, String delimiter, String encoding, List<StrLayout.Column> columns) {

    /** Null list becomes empty. */
    public Layout {
      columns = columns == null ? List.of() : List.copyOf(columns);
    }
  }
}
