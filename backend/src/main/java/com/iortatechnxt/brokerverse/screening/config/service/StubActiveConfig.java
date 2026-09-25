package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.screening.config.domain.ApproverKind;
import com.iortatechnxt.brokerverse.screening.config.domain.Balancing;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.domain.FieldDataType;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchAlgorithm;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchField;
import com.iortatechnxt.brokerverse.screening.config.domain.RiskAttribute;
import com.iortatechnxt.brokerverse.screening.config.domain.RuleOperator;
import com.iortatechnxt.brokerverse.screening.config.domain.SlaCalendar;
import com.iortatechnxt.brokerverse.screening.config.domain.StrFormat;
import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import com.iortatechnxt.brokerverse.screening.config.domain.TemplateType;
import com.iortatechnxt.brokerverse.screening.config.domain.ValidationKind;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Temporary {@link ActiveConfig} returning the seeded demo defaults (version 1 of every type, in
 * force since 2020-01-01). It lets the matching wave build against the port before the versioned
 * tables exist; the database-backed resolver replaces it in the same wave.
 */
@Component
public class StubActiveConfig implements ActiveConfig {

  private static final LocalDate SINCE = LocalDate.of(2020, 1, 1);
  private static final String SANCTION = "SANCTION";
  private static final String PEP = "PEP";

  @Override
  public Optional<ConfigVersionRef> activeVersion(
      Long companyId, ConfigType type, String scope, LocalDate asOf) {
    if (asOf.isBefore(SINCE)) {
      return Optional.empty();
    }
    return Optional.of(ref(type, scope));
  }

  private static ConfigVersionRef ref(ConfigType type, String scope) {
    long id = type.ordinal() + 1L;
    if (type == ConfigType.TEMPLATE && scope != null) {
      id = 100L + TemplateType.valueOf(scope).ordinal();
    }
    return new ConfigVersionRef(id, type, scope, 1, SINCE);
  }

  private static ConfigVersionRef require(Long versionId, ConfigType type) {
    if (versionId == null || versionId != type.ordinal() + 1L) {
      throw new ResourceNotFoundException(type + " configuration version", versionId);
    }
    return ref(type, null);
  }

  @Override
  public MatchCriteria matchCriteria(Long versionId) {
    ConfigVersionRef v = require(versionId, ConfigType.MATCH_CRITERIA);
    Set<MatchField> person = Set.of(MatchField.NAME, MatchField.ALIAS, MatchField.BIRTH_DATE);
    Set<MatchField> entity = Set.of(MatchField.NAME, MatchField.ALIAS);
    BigDecimal one = new BigDecimal("1.0000");
    List<MatchCriteria.Rule> rules =
        List.of(
            rule(SANCTION, SubjectType.INDIVIDUAL, MatchAlgorithm.EXACT, one, person, one),
            rule(SANCTION, SubjectType.INDIVIDUAL, MatchAlgorithm.PHONETIC, "0.8500", person),
            rule(SANCTION, SubjectType.INDIVIDUAL, MatchAlgorithm.FUZZY, "0.8800", person),
            rule(SANCTION, SubjectType.ENTITY, MatchAlgorithm.EXACT, one, entity, one),
            rule(SANCTION, SubjectType.ENTITY, MatchAlgorithm.FUZZY, "0.9000", entity),
            rule(PEP, SubjectType.INDIVIDUAL, MatchAlgorithm.EXACT, one, person, one),
            rule(PEP, SubjectType.INDIVIDUAL, MatchAlgorithm.FUZZY, "0.9000", person),
            rule(PEP, SubjectType.ENTITY, MatchAlgorithm.FUZZY, "0.9200", entity));
    return new MatchCriteria(v, rules);
  }

  private static MatchCriteria.Rule rule(
      String list, SubjectType s, MatchAlgorithm a, String threshold, Set<MatchField> f) {
    BigDecimal t = new BigDecimal(threshold);
    return rule(list, s, a, t, f, t.add(new BigDecimal("0.0400")));
  }

  private static MatchCriteria.Rule rule(
      String list,
      SubjectType s,
      MatchAlgorithm a,
      BigDecimal threshold,
      Set<MatchField> f,
      BigDecimal caseScore) {
    return new MatchCriteria.Rule(null, list, s, a, threshold, f, caseScore.min(BigDecimal.ONE));
  }

  @Override
  public RiskRules riskRules(Long versionId) {
    ConfigVersionRef v = require(versionId, ConfigType.RISK_RULES);
    return new RiskRules(
        v,
        List.of(
            new RiskRules.Category(
                "SANCTIONED",
                "Sanctions hit",
                1,
                "HIGH",
                Set.of("WATCHLIST_REVIEW"),
                "NAME_MATCH",
                true),
            new RiskRules.Category(
                "PEP", "Politically exposed person", 2, "HIGH", Set.of(PEP), PEP, true),
            new RiskRules.Category(
                "INTERNAL_WATCH",
                "Internal watchlist",
                3,
                "STANDARD",
                Set.of("WATCHLIST_REVIEW"),
                "MONITOR",
                false)),
        List.of(
            new RiskRules.Rule(
                null,
                10,
                "SANCTIONED",
                RiskAttribute.MATCH_LIST_TYPE,
                RuleOperator.EQ,
                Set.of(SANCTION)),
            new RiskRules.Rule(
                null, 20, PEP, RiskAttribute.MATCH_LIST_TYPE, RuleOperator.EQ, Set.of(PEP)),
            new RiskRules.Rule(null, 30, PEP, RiskAttribute.PEP, RuleOperator.EQ, Set.of("TRUE")),
            new RiskRules.Rule(
                null,
                40,
                "INTERNAL_WATCH",
                RiskAttribute.MATCH_LIST_TYPE,
                RuleOperator.IN,
                Set.of("INTERNAL", "ADVERSE_MEDIA"))));
  }

  @Override
  public ApprovalMatrix approvalMatrix(Long versionId) {
    ConfigVersionRef v = require(versionId, ConfigType.APPROVAL_MATRIX);
    return new ApprovalMatrix(
        v,
        List.of(
            new ApprovalMatrix.Route(
                null,
                10,
                "INVESTIGATION",
                null,
                null,
                null,
                "FALSE_POSITIVE",
                "CLOSED",
                null,
                null),
            new ApprovalMatrix.Route(
                null,
                20,
                "INVESTIGATION",
                null,
                null,
                null,
                null,
                "UNIT_HEAD_APPROVAL",
                ApproverKind.ROLE,
                "SCR_APPROVER"),
            new ApprovalMatrix.Route(
                null,
                30,
                "UNIT_HEAD_APPROVAL",
                null,
                null,
                null,
                null,
                "COMPLIANCE_REVIEW",
                ApproverKind.ROLE,
                "COMPLIANCE_OFFICER")));
  }

  @Override
  public AssignmentMatrix assignmentMatrix(Long versionId) {
    ConfigVersionRef v = require(versionId, ConfigType.ASSIGNMENT_MATRIX);
    return new AssignmentMatrix(
        v,
        List.of(
            new AssignmentMatrix.Rule(
                null,
                10,
                null,
                null,
                null,
                null,
                null,
                "SCR_INVESTIGATOR",
                null,
                Balancing.LEAST_OPEN)));
  }

  @Override
  public SlaMatrix slaMatrix(Long versionId) {
    ConfigVersionRef v = require(versionId, ConfigType.SLA_MATRIX);
    return new SlaMatrix(
        v,
        List.of(
            new SlaMatrix.Rule(
                null,
                "INVESTIGATION",
                null,
                null,
                72,
                24,
                "COMPLIANCE_OFFICER",
                SlaCalendar.CALENDAR)));
  }

  @Override
  public ValidationRules validationRules(Long versionId) {
    ConfigVersionRef v = require(versionId, ConfigType.VALIDATION_RULES);
    return new ValidationRules(
        v,
        List.of(
            new ValidationRules.Rule(
                null, "INVESTIGATION", null, ValidationKind.TEMPLATE_COMPLETE, null, true)));
  }

  @Override
  public ReviewTemplate template(Long versionId) {
    if (versionId == null || versionId < 100L || versionId > 103L) {
      throw new ResourceNotFoundException("TEMPLATE configuration version", versionId);
    }
    TemplateType type = TemplateType.values()[(int) (versionId - 100L)];
    return new ReviewTemplate(
        ref(ConfigType.TEMPLATE, type.name()),
        type,
        type.name(),
        List.of(
            new ReviewTemplate.Field(
                null,
                "Summary",
                "FINDINGS",
                "Findings",
                FieldDataType.LONG_TEXT,
                null,
                true,
                null,
                10,
                null)));
  }

  @Override
  public StrLayout strLayout(Long versionId) {
    ConfigVersionRef v = require(versionId, ConfigType.STR_LAYOUT);
    return new StrLayout(
        v,
        StrFormat.CSV,
        ",",
        "UTF-8",
        List.of(new StrLayout.Column(1, "STR_NO", null, "STR No.", null, null, Map.of())));
  }
}
