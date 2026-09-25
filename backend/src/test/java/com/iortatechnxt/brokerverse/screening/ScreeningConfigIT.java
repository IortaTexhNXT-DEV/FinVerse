package com.iortatechnxt.brokerverse.screening;

import static com.iortatechnxt.brokerverse.screening.ScreeningSetupFixtures.rule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.screening.config.domain.ApproverKind;
import com.iortatechnxt.brokerverse.screening.config.domain.Balancing;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigStatus;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigVersion;
import com.iortatechnxt.brokerverse.screening.config.domain.FieldDataType;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchAlgorithm;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchField;
import com.iortatechnxt.brokerverse.screening.config.domain.RiskAttribute;
import com.iortatechnxt.brokerverse.screening.config.domain.RuleOperator;
import com.iortatechnxt.brokerverse.screening.config.domain.SlaCalendar;
import com.iortatechnxt.brokerverse.screening.config.domain.StrFormat;
import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import com.iortatechnxt.brokerverse.screening.config.domain.TemplateType;
import com.iortatechnxt.brokerverse.screening.config.service.ActiveConfig;
import com.iortatechnxt.brokerverse.screening.config.service.ApprovalMatrix;
import com.iortatechnxt.brokerverse.screening.config.service.AssignmentMatrix;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigApprovalSource;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigChange;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigContent;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigVersionService;
import com.iortatechnxt.brokerverse.screening.config.service.MatchCriteria;
import com.iortatechnxt.brokerverse.screening.config.service.ReviewTemplate;
import com.iortatechnxt.brokerverse.screening.config.service.RiskRules;
import com.iortatechnxt.brokerverse.screening.config.service.SlaMatrix;
import com.iortatechnxt.brokerverse.screening.config.service.StrLayout;
import com.iortatechnxt.brokerverse.screening.config.service.ValidationRules;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Versioned screening configuration (SNSRP-101-109; FR-SS-010 to 019): draft, save with the FRS
 * validations, submit with the before / after difference, approve (ACTIVE from the effective date,
 * the previous version SUPERSEDED on that date), reject, withdraw, four eyes, the approval inbox,
 * and the {@link ActiveConfig} port over the demo configuration.
 */
@IntegrationTest
class ScreeningConfigIT {

  private static final String MAKER = "compoff";
  private static final String CHECKER = "compchk";
  private static final String DUAL = "compdual";
  private static final String SANCTION = "SANCTION";

  @Autowired private ConfigVersionService service;
  @Autowired private ActiveConfig activeConfig;
  @Autowired private ConfigApprovalSource approvals;
  @Autowired private ScreeningSetupFixtures fx;
  @Autowired private AsUser asUser;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Clock clock;

  private LocalDate today() {
    return LocalDate.now(clock);
  }

  private <T> T as(String user, Supplier<T> action) {
    return asUser.run(user, action);
  }

  private ConfigVersion saved(ConfigVersion draft, ConfigContent content) {
    return as(MAKER, () -> service.saveDraft(draft.getId(), today(), "test", content));
  }

  private static ConfigContent match(String fuzzy) {
    return ConfigContent.ofMatch(
        List.of(
            rule(SANCTION, MatchAlgorithm.EXACT, "1", "1"),
            rule(SANCTION, MatchAlgorithm.FUZZY, fuzzy, "0.95")));
  }

  private ConfigVersion activeMatchVersion(Long company) {
    ConfigVersion draft =
        as(MAKER, () -> service.newDraft(company, ConfigType.MATCH_CRITERIA, null));
    saved(draft, match("0.85"));
    as(MAKER, () -> service.submit(draft.getId()));
    return as(CHECKER, () -> service.approve(draft.getId()));
  }

  @Test
  void draftSubmitApproveMakesTheVersionActiveWithItsDifference() {
    Long company = fx.company();
    assertThat(activeConfig.matchCriteria(company, today())).isEmpty();
    ConfigVersion v1 = activeMatchVersion(company);
    assertThat(v1.getStatus()).isEqualTo(ConfigStatus.ACTIVE);
    MatchCriteria criteria = activeConfig.matchCriteria(company, today()).orElseThrow();
    assertThat(criteria.version().id()).isEqualTo(v1.getId());
    assertThat(criteria.rulesFor(SANCTION, SubjectType.INDIVIDUAL)).hasSize(2);

    ConfigVersion v2 = as(MAKER, () -> service.newDraft(company, ConfigType.MATCH_CRITERIA, null));
    assertThat(v2.getVersionNo()).isEqualTo(2);
    assertThat(service.content(v2).matchRules()).hasSize(2);
    assertThat(as(MAKER, () -> service.newDraft(company, ConfigType.MATCH_CRITERIA, null)).getId())
        .as("a second New Draft opens the existing draft")
        .isEqualTo(v2.getId());
    assertThatThrownBy(() -> as(MAKER, () -> service.submit(v2.getId())))
        .hasMessage("The draft does not change the active configuration");
    saved(v2, match("0.88"));
    as(MAKER, () -> service.submit(v2.getId()));
    List<ConfigChange> changes = service.changes(v2.getId());
    assertThat(changes)
        .contains(new ConfigChange("SANCTION INDIVIDUAL FUZZY", "Threshold", "0.8500", "0.8800"));
    as(CHECKER, () -> service.approve(v2.getId()));
    assertThat(activeConfig.matchCriteria(company, today()).orElseThrow().version().versionNo())
        .isEqualTo(2);
    assertThat(service.versions(company, ConfigType.MATCH_CRITERIA))
        .extracting(ConfigVersion::getStatus)
        .containsExactly(ConfigStatus.ACTIVE, ConfigStatus.SUPERSEDED);
  }

  @Test
  void aPendingVersionBlocksANewDraftAndTheMakerNeverApproves() {
    Long company = fx.company();
    ConfigVersion draft =
        as(DUAL, () -> service.newDraft(company, ConfigType.MATCH_CRITERIA, null));
    as(DUAL, () -> service.saveDraft(draft.getId(), today(), "dual", match("0.90")));
    as(DUAL, () -> service.submit(draft.getId()));
    assertThatThrownBy(
            () -> as(MAKER, () -> service.newDraft(company, ConfigType.MATCH_CRITERIA, null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("waiting for approval");
    assertThatThrownBy(() -> as(DUAL, () -> service.approve(draft.getId())))
        .hasMessage("A configuration change is approved by someone other than its maker");
    assertThat(
            approvals.pendingFor(ApprovalViewer.user(DUAL, Set.of("SCR_CONFIG_APPROVE"))).stream()
                .map(PendingApproval::reference))
        .doesNotContain(ConfigVersionService.label(draft));
    assertThat(
            approvals
                .pendingFor(ApprovalViewer.user(CHECKER, Set.of("SCR_CONFIG_APPROVE")))
                .stream()
                .map(PendingApproval::link))
        .contains(ConfigVersionService.link(draft));
    assertThatThrownBy(() -> as(CHECKER, () -> service.reject(draft.getId(), " ")))
        .hasMessage("Enter the reason for the rejection");
    ConfigVersion rejected = as(CHECKER, () -> service.reject(draft.getId(), "Threshold too low"));
    assertThat(rejected.getStatus()).isEqualTo(ConfigStatus.REJECTED);
    assertThat(rejected.getDecisionReason()).isEqualTo("Threshold too low");
    assertThat(activeConfig.matchCriteria(company, today())).isEmpty();
  }

  @Test
  void aFutureVersionTakesOverOnItsDateAndSupersedesThePreviousOne() {
    Long company = fx.company();
    ConfigVersion v1 = activeMatchVersion(company);
    ConfigVersion v2 = as(MAKER, () -> service.newDraft(company, ConfigType.MATCH_CRITERIA, null));
    LocalDate tomorrow = today().plusDays(1);
    as(MAKER, () -> service.saveDraft(v2.getId(), tomorrow, "tomorrow", match("0.92")));
    as(MAKER, () -> service.submit(v2.getId()));
    as(CHECKER, () -> service.approve(v2.getId()));
    assertThat(activeConfig.activeVersion(company, ConfigType.MATCH_CRITERIA, null, today()))
        .get()
        .extracting(r -> r.id())
        .isEqualTo(v1.getId());
    assertThat(activeConfig.matchCriteria(company, tomorrow).orElseThrow().version().id())
        .isEqualTo(v2.getId());
    assertThat(as(MAKER, () -> service.supersedeDue(tomorrow))).isPositive();
    assertThat(service.get(v1.getId()).getStatus()).isEqualTo(ConfigStatus.SUPERSEDED);
    assertThat(activeConfig.matchCriteria(v1.getId()).rules()).hasSize(2);
  }

  @Test
  void theMakerDiscardsADraftAndEmptyOrPastDraftsAreRefused() {
    Long company = fx.company();
    ConfigVersion draft = as(MAKER, () -> service.newDraft(company, ConfigType.SLA_MATRIX, null));
    assertThatThrownBy(() -> as(MAKER, () -> service.submit(draft.getId())))
        .hasMessage("Add at least one rule before submitting");
    assertThatThrownBy(
            () ->
                as(
                    MAKER,
                    () ->
                        service.saveDraft(
                            draft.getId(), today().minusDays(1), "past", ConfigContent.empty())))
        .hasMessage("The effective date cannot be before today");
    assertThatThrownBy(() -> as(CHECKER, () -> service.withdraw(draft.getId())))
        .hasMessage("Only the maker of the draft can discard it");
    ConfigVersion withdrawn = as(MAKER, () -> service.withdraw(draft.getId()));
    assertThat(withdrawn.getStatus()).isEqualTo(ConfigStatus.REJECTED);
    assertThat(withdrawn.getDecisionReason()).isEqualTo(ConfigVersion.WITHDRAWN);
    assertThatThrownBy(() -> saved(withdrawn, ConfigContent.empty()))
        .hasMessageContaining("only a draft can be changed");
  }

  @Test
  void matchingAndRiskRowsAreValidatedWithTheFrsMessages() {
    Long company = fx.company();
    ConfigVersion match =
        as(MAKER, () -> service.newDraft(company, ConfigType.MATCH_CRITERIA, null));
    assertThatThrownBy(
            () ->
                saved(
                    match,
                    ConfigContent.ofMatch(
                        List.of(rule(SANCTION, MatchAlgorithm.FUZZY, "1.5", "1.5")))))
        .hasMessage("Enter a threshold between 0 and 1");
    assertThatThrownBy(
            () ->
                saved(
                    match,
                    ConfigContent.ofMatch(
                        List.of(rule(SANCTION, MatchAlgorithm.FUZZY, "0.90", "0.80")))))
        .hasMessage("The case threshold must be at least the matching threshold");
    MatchCriteria.Rule noName =
        new MatchCriteria.Rule(
            null,
            SANCTION,
            SubjectType.ENTITY,
            MatchAlgorithm.EXACT,
            BigDecimal.ONE,
            Set.of(MatchField.ALIAS),
            BigDecimal.ONE);
    assertThatThrownBy(() -> saved(match, ConfigContent.ofMatch(List.of(noName))))
        .hasMessage("Select at least the name field");
    saved(
        match,
        ConfigContent.ofMatch(
            List.of(
                rule(SANCTION, MatchAlgorithm.EXACT, "1.0000", "1.0000"),
                rule("PEP", MatchAlgorithm.FUZZY, "0.0000", "0.5"))));

    ConfigVersion risk = as(MAKER, () -> service.newDraft(company, ConfigType.RISK_RULES, null));
    RiskRules.Category high =
        new RiskRules.Category("UAT_H", "High", 1, "HIGH", Set.of("PEP"), "PEP", true);
    RiskRules.Rule r10 =
        new RiskRules.Rule(null, 10, "UAT_H", RiskAttribute.PEP, RuleOperator.EQ, Set.of("TRUE"));
    assertThatThrownBy(
            () ->
                saved(
                    risk,
                    ConfigContent.ofRisk(
                        List.of(
                            new RiskRules.Category(
                                "UAT_X", "X", 1, "VERY_HIGH", Set.of(), null, false)),
                        List.of())))
        .hasMessage("Risk rating VERY_HIGH is not a value of KYC_RISK_RATING");
    assertThatThrownBy(() -> saved(risk, ConfigContent.ofRisk(List.of(high), List.of(r10, r10))))
        .hasMessage("Each rule needs its own priority");
    RiskRules.Rule missing =
        new RiskRules.Rule(
            null, 20, "UAT_MISSING", RiskAttribute.PEP, RuleOperator.EQ, Set.of("TRUE"));
    assertThatThrownBy(
            () -> saved(risk, ConfigContent.ofRisk(List.of(high), List.of(r10, missing))))
        .hasMessage("Category UAT_MISSING is not defined in this version");
    saved(risk, ConfigContent.ofRisk(List.of(high), List.of(r10)));
    assertThat(service.content(service.get(risk.getId())).riskRules()).hasSize(1);
  }

  @Test
  void matricesTemplatesAndLayoutsAreValidatedWithTheFrsMessages() {
    Long company = fx.company();
    ConfigVersion routes =
        as(MAKER, () -> service.newDraft(company, ConfigType.APPROVAL_MATRIX, null));
    assertThatThrownBy(
            () ->
                saved(
                    routes,
                    ConfigContent.ofRoutes(
                        List.of(route(10, "UNIT_HEAD_APPROVAL", "STR_EXTRACTION", null, null)))))
        .hasMessage("Stage STR_EXTRACTION cannot follow UNIT_HEAD_APPROVAL");
    assertThatThrownBy(
            () ->
                saved(
                    routes,
                    ConfigContent.ofRoutes(List.of(route(10, "INVESTIGATION", null, null, null)))))
        .hasMessage("Select the next stage");
    assertThatThrownBy(
            () ->
                saved(
                    routes,
                    ConfigContent.ofRoutes(
                        List.of(
                            route(
                                10,
                                "INVESTIGATION",
                                "UNIT_HEAD_APPROVAL",
                                ApproverKind.USER,
                                null)))))
        .hasMessage("Select the approving user");

    ConfigVersion assign =
        as(MAKER, () -> service.newDraft(company, ConfigType.ASSIGNMENT_MATRIX, null));
    assertThatThrownBy(
            () -> saved(assign, ConfigContent.ofAssignments(List.of(scenario(null, null)))))
        .hasMessage("Select a team role or a user");
    assertThatThrownBy(
            () -> saved(assign, ConfigContent.ofAssignments(List.of(scenario(null, CHECKER)))))
        .hasMessage("User compchk cannot investigate cases");
    saved(assign, ConfigContent.ofAssignments(List.of(scenario(null, "investigator"))));

    ConfigVersion sla = as(MAKER, () -> service.newDraft(company, ConfigType.SLA_MATRIX, null));
    assertThatThrownBy(() -> saved(sla, ConfigContent.ofSla(List.of(slaRow(0, 0)))))
        .hasMessage("Enter the SLA in hours (greater than 0)");
    assertThatThrownBy(() -> saved(sla, ConfigContent.ofSla(List.of(slaRow(24, 24)))))
        .hasMessage("The reminder must fall before the SLA ends");
    saved(sla, ConfigContent.ofSla(List.of(slaRow(24, 23))));

    ConfigVersion template =
        as(MAKER, () -> service.newDraft(company, ConfigType.TEMPLATE, "KYC_REVIEW"));
    assertThatThrownBy(
            () -> saved(template, templateOf(field("UAT_F1", null, FieldDataType.TEXT, true))))
        .hasMessage("Enter the field label");
    assertThatThrownBy(
            () -> saved(template, templateOf(field("UAT_F2", "L", FieldDataType.LOV, true))))
        .hasMessage("Select the list of values of the field");
    assertThatThrownBy(
            () -> saved(template, templateOf(field("UAT_F3", "L", FieldDataType.TEXT, false))))
        .hasMessage("A review template needs at least one mandatory field");

    ConfigVersion layout = as(MAKER, () -> service.newDraft(company, ConfigType.STR_LAYOUT, null));
    assertThatThrownBy(
            () ->
                saved(
                    layout,
                    layoutOf(
                        StrFormat.CSV,
                        new StrLayout.Column(5, null, null, "H", null, null, Map.of()))))
        .hasMessage("Column 5 needs an STR field or a fixed value");
    assertThatThrownBy(
            () ->
                saved(
                    layout,
                    layoutOf(
                        StrFormat.FIXED,
                        new StrLayout.Column(1, "STR_NO", null, "H", null, "LEFT", Map.of()))))
        .hasMessage("Enter the column length");
  }

  @Test
  void aTemplateVersionIsKeptPerTemplateTypeAndReadByVersionId() {
    Long company = fx.company();
    ConfigVersion kyc =
        as(MAKER, () -> service.newDraft(company, ConfigType.TEMPLATE, "KYC_REVIEW"));
    saved(
        kyc,
        templateOf(field("SOURCE_OF_WEALTH", "Source of wealth", FieldDataType.LONG_TEXT, true)));
    as(MAKER, () -> service.submit(kyc.getId()));
    as(CHECKER, () -> service.approve(kyc.getId()));
    ReviewTemplate active =
        activeConfig.template(company, TemplateType.KYC_REVIEW, today()).orElseThrow();
    assertThat(active.fields())
        .extracting(ReviewTemplate.Field::code)
        .containsExactly("SOURCE_OF_WEALTH");
    assertThat(activeConfig.template(company, TemplateType.EDD, today())).isEmpty();
    assertThat(activeConfig.template(kyc.getId()).templateType())
        .isEqualTo(TemplateType.KYC_REVIEW);
    assertThatThrownBy(() -> as(MAKER, () -> service.newDraft(company, ConfigType.TEMPLATE, null)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void theDemoConfigurationIsActiveForTheDemoCompany() {
    Long fvi = jdbc.queryForObject("select id from org_company where code = 'FVI'", Long.class);
    LocalDate day = LocalDate.of(2026, 6, 30);
    MatchCriteria match = activeConfig.matchCriteria(fvi, day).orElseThrow();
    assertThat(match.rulesFor(SANCTION, SubjectType.INDIVIDUAL))
        .extracting(MatchCriteria.Rule::algorithm)
        .containsExactly(MatchAlgorithm.EXACT, MatchAlgorithm.PHONETIC, MatchAlgorithm.FUZZY);
    RiskRules risk = activeConfig.riskRules(fvi, day).orElseThrow();
    assertThat(risk.categories()).hasSize(3);
    assertThat(risk.rules().get(0).test(List.of(SANCTION))).isTrue();
    assertThat(risk.category("PEP").orElseThrow().requiresEdd()).isTrue();
    ApprovalMatrix routes = activeConfig.approvalMatrix(fvi, day).orElseThrow();
    assertThat(routes.from("COMPLIANCE_REVIEW")).hasSize(4);
    AssignmentMatrix assignment = activeConfig.assignmentMatrix(fvi, day).orElseThrow();
    assertThat(assignment.rules().get(0).balancing()).isEqualTo(Balancing.ROUND_ROBIN);
    SlaMatrix sla = activeConfig.slaMatrix(fvi, day).orElseThrow();
    assertThat(sla.ruleFor("INVESTIGATION", "PEP", null).orElseThrow().slaHours()).isEqualTo(24);
    assertThat(sla.ruleFor("INVESTIGATION", "NAME_MATCH", null).orElseThrow().slaHours())
        .isEqualTo(72);
    ValidationRules checks = activeConfig.validationRules(fvi, day).orElseThrow();
    assertThat(checks.rulesFor("INVESTIGATION", "PEP")).hasSize(4);
    assertThat(activeConfig.template(fvi, TemplateType.STR, day).orElseThrow().fields())
        .anyMatch(f -> "CLIENT_NAME".equals(f.prefillSource()));
    StrLayout layout = activeConfig.strLayout(fvi, day).orElseThrow();
    assertThat(layout.columns()).hasSize(5);
    assertThat(layout.format()).isEqualTo(StrFormat.CSV);
    assertThat(activeConfig.slaMatrix(fvi, LocalDate.of(2025, 12, 31))).isEmpty();
    assertThat(
            approvals
                .pendingFor(ApprovalViewer.user(CHECKER, Set.of("SCR_CONFIG_APPROVE")))
                .stream()
                .map(PendingApproval::reference))
        .contains("SLA_MATRIX v2");
    assertThat(approvals.pendingFor(ApprovalViewer.user(MAKER, Set.of("SCR_CONFIG_MAINTAIN"))))
        .isEmpty();
  }

  private static ApprovalMatrix.Route route(
      int order, String from, String to, ApproverKind kind, String value) {
    return new ApprovalMatrix.Route(null, order, from, null, null, null, null, to, kind, value);
  }

  private static AssignmentMatrix.Rule scenario(String role, String user) {
    return new AssignmentMatrix.Rule(
        null, 10, "PEP", null, null, null, null, role, user, Balancing.NONE);
  }

  private static SlaMatrix.Rule slaRow(int hours, int lead) {
    return new SlaMatrix.Rule(
        null, "INVESTIGATION", null, null, hours, lead, "COMPLIANCE_OFFICER", SlaCalendar.CALENDAR);
  }

  private static ReviewTemplate.Field field(
      String code, String label, FieldDataType type, boolean mandatory) {
    return new ReviewTemplate.Field(
        null, "Section", code, label, type, null, mandatory, null, 10, null);
  }

  private static ConfigContent templateOf(ReviewTemplate.Field field) {
    return ConfigContent.ofTemplate(
        new ConfigContent.Template(TemplateType.KYC_REVIEW, "KYC test", List.of(field)));
  }

  private static ConfigContent layoutOf(StrFormat format, StrLayout.Column column) {
    return ConfigContent.ofLayout(new ConfigContent.Layout(format, ",", "UTF-8", List.of(column)));
  }
}
