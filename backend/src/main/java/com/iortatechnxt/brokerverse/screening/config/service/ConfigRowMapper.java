package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.screening.config.domain.ApprovalRouteRow;
import com.iortatechnxt.brokerverse.screening.config.domain.AssignmentRuleRow;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchField;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchRuleRow;
import com.iortatechnxt.brokerverse.screening.config.domain.RiskCategoryRow;
import com.iortatechnxt.brokerverse.screening.config.domain.RiskRuleRow;
import com.iortatechnxt.brokerverse.screening.config.domain.SlaRuleRow;
import com.iortatechnxt.brokerverse.screening.config.domain.StrLayoutColumnRow;
import com.iortatechnxt.brokerverse.screening.config.domain.TemplateFieldRow;
import com.iortatechnxt.brokerverse.screening.config.domain.ValidationRuleRow;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Converts configuration rows to the immutable snapshot records of {@link ActiveConfig} and back.
 * Multi-valued attributes are stored as sorted, comma-separated codes; the STR code map as {@code
 * CODE=VALUE;...}.
 */
final class ConfigRowMapper {

  private static final String COMMA = ",";
  private static final String PAIRS = ";";
  private static final String PAIR = "=";

  private ConfigRowMapper() {}

  static MatchCriteria.Rule toView(MatchRuleRow r) {
    Set<MatchField> fields =
        codes(r.getMatchFields()).stream().map(MatchField::valueOf).collect(Collectors.toSet());
    return new MatchCriteria.Rule(
        r.getId(),
        r.getListType(),
        r.getSubjectType(),
        r.getAlgorithm(),
        r.getThreshold(),
        fields,
        r.getMinScoreForCase());
  }

  static MatchRuleRow toRow(Long versionId, int order, MatchCriteria.Rule r) {
    return new MatchRuleRow(
        versionId,
        order,
        r.listType(),
        r.subjectType(),
        r.algorithm(),
        r.threshold(),
        join(r.fields().stream().map(Enum::name).toList()),
        r.minScoreForCase());
  }

  static RiskRules.Category toView(RiskCategoryRow r) {
    return new RiskRules.Category(
        r.getCode(),
        r.getName(),
        r.getTier(),
        r.getKycRiskRating(),
        codes(r.getTags()),
        r.getCaseType(),
        r.isRequiresEdd());
  }

  static RiskCategoryRow toRow(Long versionId, RiskRules.Category c) {
    return new RiskCategoryRow(
        versionId,
        c.code(),
        c.name(),
        c.tier(),
        c.kycRiskRating(),
        join(c.tags()),
        blankToNull(c.caseType()),
        c.requiresEdd());
  }

  static RiskRules.Rule toView(RiskRuleRow r) {
    return new RiskRules.Rule(
        r.getId(),
        r.getPriority(),
        r.getCategoryCode(),
        r.getConditionAttr(),
        r.getOperator(),
        codes(r.getRuleValues()));
  }

  static RiskRuleRow toRow(Long versionId, RiskRules.Rule r) {
    return new RiskRuleRow(
        versionId, r.priority(), r.categoryCode(), r.attribute(), r.operator(), join(r.values()));
  }

  static ApprovalMatrix.Route toView(ApprovalRouteRow r) {
    return new ApprovalMatrix.Route(
        r.getId(),
        r.getSortOrder(),
        r.getFromStage(),
        r.getCaseType(),
        r.getRiskCategory(),
        r.getMarketingUnit(),
        r.getDisposition(),
        r.getToStage(),
        r.getApproverKind(),
        r.getApproverValue());
  }

  static ApprovalRouteRow toRow(Long versionId, ApprovalMatrix.Route r) {
    return new ApprovalRouteRow(
        versionId,
        r.order(),
        r.fromStage(),
        blankToNull(r.caseType()),
        blankToNull(r.riskCategory()),
        blankToNull(r.marketingUnit()),
        blankToNull(r.disposition()),
        r.toStage(),
        r.approverKind(),
        blankToNull(r.approverValue()));
  }

  static AssignmentMatrix.Rule toView(AssignmentRuleRow r) {
    return new AssignmentMatrix.Rule(
        r.getId(),
        r.getSortOrder(),
        r.getCaseType(),
        r.getTriggerCode(),
        r.getRiskCategory(),
        r.getMarketingUnit(),
        r.getClientType(),
        r.getTeamRole(),
        r.getUserName(),
        r.getBalancing());
  }

  static AssignmentRuleRow toRow(Long versionId, AssignmentMatrix.Rule r) {
    return new AssignmentRuleRow(
        versionId,
        r.order(),
        blankToNull(r.caseType()),
        blankToNull(r.trigger()),
        blankToNull(r.riskCategory()),
        blankToNull(r.marketingUnit()),
        blankToNull(r.clientType()),
        blankToNull(r.teamRole()),
        blankToNull(r.user()),
        r.balancing());
  }

  static SlaMatrix.Rule toView(SlaRuleRow r) {
    return new SlaMatrix.Rule(
        r.getId(),
        r.getStage(),
        r.getCaseType(),
        r.getRiskCategory(),
        r.getSlaHours(),
        r.getReminderLeadHours(),
        r.getEscalateToRole(),
        r.getCalendar());
  }

  static SlaRuleRow toRow(Long versionId, SlaMatrix.Rule r) {
    return new SlaRuleRow(
        versionId,
        r.stage(),
        blankToNull(r.caseType()),
        blankToNull(r.riskCategory()),
        r.slaHours(),
        r.reminderLeadHours(),
        r.escalateToRole(),
        r.calendar());
  }

  static ValidationRules.Rule toView(ValidationRuleRow r) {
    return new ValidationRules.Rule(
        r.getId(),
        r.getStage(),
        r.getCaseType(),
        r.getRuleKind(),
        r.getParameters(),
        r.isBlocking());
  }

  static ValidationRuleRow toRow(Long versionId, ValidationRules.Rule r) {
    return new ValidationRuleRow(
        versionId,
        blankToNull(r.stage()),
        blankToNull(r.caseType()),
        r.rule(),
        blankToNull(r.parameters()),
        r.blocking());
  }

  static ReviewTemplate.Field toView(TemplateFieldRow r) {
    return new ReviewTemplate.Field(
        r.getId(),
        r.getSection(),
        r.getCode(),
        r.getLabel(),
        r.getDataType(),
        r.getLovType(),
        r.isMandatory(),
        r.getHelpText(),
        r.getSortOrder(),
        r.getPrefillSource());
  }

  static TemplateFieldRow toRow(Long templateId, ReviewTemplate.Field f) {
    return new TemplateFieldRow(
        templateId,
        f.section(),
        f.code(),
        f.label(),
        f.dataType(),
        blankToNull(f.lovType()),
        f.mandatory(),
        blankToNull(f.help()),
        f.order(),
        blankToNull(f.prefillSource()));
  }

  static StrLayout.Column toView(StrLayoutColumnRow r) {
    return new StrLayout.Column(
        r.getSortOrder(),
        r.getFieldCode(),
        r.getFixedValue(),
        r.getHeader(),
        r.getLength(),
        r.getPad(),
        codeMap(r.getCodeMap()));
  }

  static StrLayoutColumnRow toRow(Long layoutId, StrLayout.Column c) {
    String map =
        c.codeMap().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + PAIR + e.getValue())
            .collect(Collectors.joining(PAIRS));
    return new StrLayoutColumnRow(
        layoutId,
        c.order(),
        blankToNull(c.fieldCode()),
        blankToNull(c.fixedValue()),
        blankToNull(c.header()),
        c.length(),
        blankToNull(c.pad()),
        map);
  }

  /**
   * Sorted comma-separated codes.
   *
   * @param codes codes
   * @return joined text, '' when none
   */
  static String join(Collection<String> codes) {
    return String.join(COMMA, new TreeSet<>(codes));
  }

  static Set<String> codes(String text) {
    if (text == null || text.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(text.split(COMMA))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private static Map<String, String> codeMap(String text) {
    Map<String, String> map = new LinkedHashMap<>();
    if (text != null && !text.isBlank()) {
      for (String pair : text.split(PAIRS)) {
        int at = pair.indexOf(PAIR);
        if (at > 0) {
          map.put(pair.substring(0, at), pair.substring(at + 1));
        }
      }
    }
    return map;
  }

  static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
