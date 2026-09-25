package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.screening.config.domain.ApproverKind;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.domain.FieldDataType;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchField;
import com.iortatechnxt.brokerverse.screening.config.domain.StrFormat;
import com.iortatechnxt.brokerverse.screening.config.domain.ValidationKind;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowTransition;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowTransitionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks the rows of a draft when it is saved (FR-SS-011 to FR-SS-017 validations). The messages
 * are those of the FRS; each failure is a {@link BusinessRuleException} naming the row.
 */
@Component
@Transactional(readOnly = true)
public class ConfigValidator {

  /** Workflow of screening cases (stage codes of the matrices). */
  static final String CASE_WORKFLOW = "SCR_CASE";

  private static final String INVESTIGATE = "SCR_INVESTIGATE";
  private static final Pattern FIELD_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,39}");

  private final LovService lovs;
  private final UserDirectory users;
  private final WorkflowTransitionRepository transitions;

  /**
   * Creates the validator.
   *
   * @param lovs lists of values
   * @param users user directory (investigators)
   * @param transitions workflow transitions of SCR_CASE
   */
  public ConfigValidator(
      LovService lovs, UserDirectory users, WorkflowTransitionRepository transitions) {
    this.lovs = lovs;
    this.users = users;
    this.transitions = transitions;
  }

  /**
   * Validates the content of a type.
   *
   * @param type type
   * @param content content
   * @param today business date (list values must be usable on it)
   */
  public void validate(ConfigType type, ConfigContent content, LocalDate today) {
    switch (type) {
      case MATCH_CRITERIA -> matchCriteria(content.matchRules(), today);
      case RISK_RULES -> riskRules(content, today);
      case APPROVAL_MATRIX -> routes(content.routes());
      case ASSIGNMENT_MATRIX -> assignments(content.assignmentRules());
      case SLA_MATRIX -> content.slaRules().forEach(ConfigValidator::sla);
      case VALIDATION_RULES -> content.validationRules().forEach(ConfigValidator::validation);
      case TEMPLATE -> template(content.template());
      case STR_LAYOUT -> layout(content.layout());
    }
  }

  private void matchCriteria(List<MatchCriteria.Rule> rules, LocalDate today) {
    Set<String> listTypes = codes("SCR_LIST_TYPE", today);
    rules.forEach(r -> matchRule(r, listTypes));
    unique(
        rules,
        r -> r.listType() + " " + r.subjectType() + " " + r.algorithm(),
        "Each list type, subject type and algorithm has one rule");
  }

  private static void matchRule(MatchCriteria.Rule r, Set<String> listTypes) {
    require(
        r.listType() != null && listTypes.contains(r.listType()),
        "SCR_LIST_TYPE_REQUIRED",
        "Select the list type of each matching rule");
    require(
        r.subjectType() != null && r.algorithm() != null,
        "SCR_MATCH_RULE_INCOMPLETE",
        "Select the subject type and the algorithm of each rule");
    require(inRange(r.threshold()), "SCR_THRESHOLD_RANGE", "Enter a threshold between 0 and 1");
    require(
        inRange(r.minScoreForCase()), "SCR_THRESHOLD_RANGE", "Enter a threshold between 0 and 1");
    require(
        r.minScoreForCase().compareTo(r.threshold()) >= 0,
        "SCR_CASE_THRESHOLD_BELOW",
        "The case threshold must be at least the matching threshold");
    require(
        r.fields().contains(MatchField.NAME),
        "SCR_NAME_FIELD_REQUIRED",
        "Select at least the name field");
  }

  private static boolean inRange(BigDecimal value) {
    return value != null && value.signum() >= 0 && value.compareTo(BigDecimal.ONE) <= 0;
  }

  private void riskRules(ConfigContent content, LocalDate today) {
    Set<String> ratings = codes("KYC_RISK_RATING", today);
    Set<String> tags = codes("CLIENT_TAG", today);
    Set<String> caseTypes = codes("SCR_CASE_TYPE", today);
    content.riskCategories().forEach(c -> category(c, ratings, tags, caseTypes));
    unique(
        content.riskCategories(),
        RiskRules.Category::code,
        "Each category code is used once in the version");
    Set<String> defined =
        content.riskCategories().stream().map(RiskRules.Category::code).collect(Collectors.toSet());
    for (RiskRules.Rule r : content.riskRules()) {
      require(
          defined.contains(r.categoryCode()),
          "SCR_CATEGORY_UNKNOWN",
          "Category " + r.categoryCode() + " is not defined in this version");
      require(
          r.attribute() != null && r.operator() != null && !r.values().isEmpty(),
          "SCR_RULE_INCOMPLETE",
          "Select the attribute, the operator and at least one value");
    }
    unique(
        content.riskRules(), r -> String.valueOf(r.priority()), "Each rule needs its own priority");
  }

  private static void category(
      RiskRules.Category c, Set<String> ratings, Set<String> tags, Set<String> caseTypes) {
    require(
        !blank(c.code()) && !blank(c.name()),
        "SCR_CATEGORY_INCOMPLETE",
        "Enter the code and name of each category");
    require(
        ratings.contains(c.kycRiskRating()),
        "SCR_RISK_RATING_UNKNOWN",
        "Risk rating " + c.kycRiskRating() + " is not a value of KYC_RISK_RATING");
    require(
        tags.containsAll(c.tags()),
        "SCR_CLIENT_TAG_UNKNOWN",
        "Tags of category " + c.code() + " must be values of CLIENT_TAG");
    require(
        blank(c.caseType()) || caseTypes.contains(c.caseType()),
        "SCR_CASE_TYPE_UNKNOWN",
        "Case type " + c.caseType() + " is not a value of SCR_CASE_TYPE");
    require(c.tier() > 0, "SCR_CATEGORY_TIER", "The tier is a whole number from 1");
  }

  private void routes(List<ApprovalMatrix.Route> routes) {
    Set<String> allowed =
        transitions.findByWorkflowCodeOrderByFromStageAscSortOrderAsc(CASE_WORKFLOW).stream()
            .map(ConfigValidator::pair)
            .collect(Collectors.toSet());
    routes.forEach(r -> route(r, allowed));
    unique(routes, r -> String.valueOf(r.order()), "Each route needs its own order");
  }

  private static void route(ApprovalMatrix.Route r, Set<String> allowed) {
    require(!blank(r.fromStage()), "SCR_ROUTE_FROM_REQUIRED", "Select the stage the route leaves");
    require(!blank(r.toStage()), "SCR_ROUTE_STAGE_REQUIRED", "Select the next stage");
    require(
        allowed.contains(r.fromStage() + ">" + r.toStage()),
        "SCR_ROUTE_STAGE_SEQUENCE",
        "Stage " + r.toStage() + " cannot follow " + r.fromStage());
    require(
        r.approverKind() != ApproverKind.USER || !blank(r.approverValue()),
        "SCR_ROUTE_USER_REQUIRED",
        "Select the approving user");
    require(
        r.approverKind() != ApproverKind.ROLE || !blank(r.approverValue()),
        "SCR_ROUTE_ROLE_REQUIRED",
        "Select the approving role");
  }

  private static String pair(WorkflowTransition t) {
    return t.getFromStage() + ">" + t.getToStage();
  }

  private void assignments(List<AssignmentMatrix.Rule> rules) {
    Set<String> investigators = new HashSet<>();
    users.usersWithPermission(INVESTIGATE).forEach(u -> investigators.add(u.toLowerCase()));
    for (AssignmentMatrix.Rule r : rules) {
      require(
          !blank(r.teamRole()) || !blank(r.user()),
          "SCR_ASSIGN_TARGET_REQUIRED",
          "Select a team role or a user");
      require(
          blank(r.user()) || investigators.contains(r.user().trim().toLowerCase()),
          "SCR_ASSIGN_USER_NOT_INVESTIGATOR",
          "User " + r.user() + " cannot investigate cases");
      require(r.balancing() != null, "SCR_ASSIGN_BALANCING_REQUIRED", "Select the balancing");
    }
    unique(rules, r -> String.valueOf(r.order()), "Each scenario needs its own order");
  }

  private static void sla(SlaMatrix.Rule r) {
    require(!blank(r.stage()), "SCR_SLA_STAGE_REQUIRED", "Select the stage of each SLA row");
    require(r.slaHours() > 0, "SCR_SLA_HOURS", "Enter the SLA in hours (greater than 0)");
    require(
        r.reminderLeadHours() >= 0 && r.reminderLeadHours() < r.slaHours(),
        "SCR_SLA_REMINDER",
        "The reminder must fall before the SLA ends");
    require(
        !blank(r.escalateToRole()) && r.calendar() != null,
        "SCR_SLA_INCOMPLETE",
        "Select the escalation role and the calendar");
  }

  private static void validation(ValidationRules.Rule r) {
    require(r.rule() != null, "SCR_VALIDATION_RULE_REQUIRED", "Select the check of each rule");
    require(
        r.rule() != ValidationKind.DOCUMENT_TYPES_PRESENT || !blank(r.parameters()),
        "SCR_VALIDATION_DOCUMENTS_REQUIRED",
        "List the document types the check requires");
  }

  private static void template(ConfigContent.Template t) {
    if (t == null || t.templateType() == null || blank(t.name())) {
      throw new BusinessRuleException("SCR_TEMPLATE_INCOMPLETE", "Enter the template name");
    }
    t.fields().forEach(ConfigValidator::field);
    unique(t.fields(), ReviewTemplate.Field::code, "A field code is used once in the template");
    require(
        t.fields().stream().anyMatch(ReviewTemplate.Field::mandatory),
        "SCR_TEMPLATE_NO_MANDATORY",
        "A review template needs at least one mandatory field");
  }

  private static void field(ReviewTemplate.Field f) {
    require(
        f.code() != null && FIELD_CODE.matcher(f.code()).matches(),
        "SCR_FIELD_CODE",
        "Field codes use capital letters, digits and underscore");
    require(!blank(f.label()), "SCR_FIELD_LABEL_REQUIRED", "Enter the field label");
    require(!blank(f.section()), "SCR_FIELD_SECTION_REQUIRED", "Enter the section of the field");
    require(f.dataType() != null, "SCR_FIELD_TYPE_REQUIRED", "Select the data type of the field");
    require(
        f.dataType() != FieldDataType.LOV || !blank(f.lovType()),
        "SCR_FIELD_LOV_REQUIRED",
        "Select the list of values of the field");
  }

  private static void layout(ConfigContent.Layout l) {
    if (l == null || l.format() == null || blank(l.encoding())) {
      throw new BusinessRuleException(
          "SCR_LAYOUT_INCOMPLETE", "Select the format and the encoding of the layout");
    }
    for (StrLayout.Column c : l.columns()) {
      require(
          !blank(c.fieldCode()) || !blank(c.fixedValue()),
          "SCR_LAYOUT_COLUMN_SOURCE",
          "Column " + c.order() + " needs an STR field or a fixed value");
      require(
          l.format() != StrFormat.FIXED || c.length() != null && c.length() > 0,
          "SCR_LAYOUT_COLUMN_LENGTH",
          "Enter the column length");
    }
    unique(l.columns(), c -> String.valueOf(c.order()), "Each column needs its own order");
  }

  private Set<String> codes(String type, LocalDate today) {
    return lovs.activeValues(type, today).stream()
        .map(LovValue::getCode)
        .collect(Collectors.toSet());
  }

  private static <T> void unique(Collection<T> rows, Function<T, String> key, String message) {
    Set<String> seen = new HashSet<>();
    for (T row : rows) {
      require(seen.add(Objects.toString(key.apply(row))), "SCR_CONFIG_DUPLICATE_ROW", message);
    }
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static void require(boolean condition, String code, String message) {
    if (!condition) {
      throw new BusinessRuleException(code, message);
    }
  }
}
