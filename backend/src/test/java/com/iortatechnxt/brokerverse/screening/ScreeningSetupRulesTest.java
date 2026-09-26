package com.iortatechnxt.brokerverse.screening;

import static com.iortatechnxt.brokerverse.screening.ScreeningSetupFixtures.rule;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.domain.FieldDataType;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchAlgorithm;
import com.iortatechnxt.brokerverse.screening.config.domain.RiskAttribute;
import com.iortatechnxt.brokerverse.screening.config.domain.RuleOperator;
import com.iortatechnxt.brokerverse.screening.config.domain.SlaCalendar;
import com.iortatechnxt.brokerverse.screening.config.domain.StrFormat;
import com.iortatechnxt.brokerverse.screening.config.domain.TemplateType;
import com.iortatechnxt.brokerverse.screening.config.domain.ValidationKind;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigChange;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigContent;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigDiff;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigVersionRef;
import com.iortatechnxt.brokerverse.screening.config.service.ReviewTemplate;
import com.iortatechnxt.brokerverse.screening.config.service.RiskRules;
import com.iortatechnxt.brokerverse.screening.config.service.SlaMatrix;
import com.iortatechnxt.brokerverse.screening.config.service.StrLayout;
import com.iortatechnxt.brokerverse.screening.config.service.ValidationRules;
import com.iortatechnxt.brokerverse.screening.watchlist.service.ListRecord;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Pure rules of Compliance Setup: the configuration difference (SNSRP-101, 108 before / after), the
 * rule helpers of the {@code ActiveConfig} snapshots and the reading of list file records
 * (SNSRP-201, 202 messages).
 */
class ScreeningSetupRulesTest {

  private static final ConfigVersionRef REF =
      new ConfigVersionRef(1L, ConfigType.SLA_MATRIX, null, 1, LocalDate.of(2026, 1, 1));

  @Test
  void theDifferenceListsChangedAddedAndRemovedRows() {
    ConfigContent before =
        ConfigContent.ofMatch(
            List.of(
                rule("SANCTION", MatchAlgorithm.FUZZY, "0.85", "0.90"),
                rule("PEP", MatchAlgorithm.EXACT, "1", "1")));
    ConfigContent after =
        ConfigContent.ofMatch(
            List.of(
                rule("SANCTION", MatchAlgorithm.FUZZY, "0.88", "0.90"),
                rule("INTERNAL", MatchAlgorithm.EXACT, "1", "1")));
    List<ConfigChange> changes = ConfigDiff.between(ConfigType.MATCH_CRITERIA, before, after);
    assertThat(changes)
        .contains(new ConfigChange("SANCTION INDIVIDUAL FUZZY", "Threshold", "0.8500", "0.8800"))
        .contains(new ConfigChange("PEP INDIVIDUAL EXACT", "Threshold", "1.0000", null))
        .contains(new ConfigChange("INTERNAL INDIVIDUAL EXACT", "Threshold", null, "1.0000"));
    assertThat(ConfigDiff.between(ConfigType.MATCH_CRITERIA, before, before)).isEmpty();
  }

  @Test
  void everyTypeHasItsDifferenceLines() {
    ConfigContent risk =
        ConfigContent.ofRisk(
            List.of(new RiskRules.Category("C", "Cat", 1, "HIGH", Set.of("PEP"), null, true)),
            List.of(
                new RiskRules.Rule(
                    null, 10, "C", RiskAttribute.PEP, RuleOperator.EQ, Set.of("TRUE"))));
    assertThat(ConfigDiff.between(ConfigType.RISK_RULES, ConfigContent.empty(), risk))
        .extracting(ConfigChange::item)
        .contains("Category C", "Rule 10");
    ConfigContent sla =
        ConfigContent.ofSla(
            List.of(
                new SlaMatrix.Rule(
                    null, "INVESTIGATION", "PEP", null, 24, 8, "R", SlaCalendar.WORKING)));
    assertThat(ConfigDiff.between(ConfigType.SLA_MATRIX, ConfigContent.empty(), sla))
        .extracting(ConfigChange::item)
        .containsOnly("SLA INVESTIGATION / PEP / *");
    ConfigContent checks =
        ConfigContent.ofValidation(
            List.of(
                new ValidationRules.Rule(
                    null, "INVESTIGATION", null, ValidationKind.TEMPLATE_COMPLETE, null, true)));
    assertThat(ConfigDiff.between(ConfigType.VALIDATION_RULES, ConfigContent.empty(), checks))
        .hasSize(2);
    ConfigContent template =
        ConfigContent.ofTemplate(
            new ConfigContent.Template(
                TemplateType.EDD,
                "EDD",
                List.of(
                    new ReviewTemplate.Field(
                        null, "S", "F", "L", FieldDataType.TEXT, null, true, null, 1, null))));
    assertThat(ConfigDiff.between(ConfigType.TEMPLATE, ConfigContent.ofTemplate(null), template))
        .extracting(ConfigChange::item)
        .contains("Template", "Field F");
    ConfigContent layout =
        ConfigContent.ofLayout(
            new ConfigContent.Layout(
                StrFormat.CSV,
                ",",
                "UTF-8",
                List.of(
                    new StrLayout.Column(1, "STR_NO", null, "No", null, null, Map.of("A", "B")))));
    assertThat(ConfigDiff.between(ConfigType.STR_LAYOUT, ConfigContent.ofLayout(null), layout))
        .extracting(ConfigChange::item)
        .contains("Layout", "Column 1");
    assertThat(template.isEmptyFor(ConfigType.TEMPLATE)).isFalse();
    assertThat(ConfigContent.empty().isEmptyFor(ConfigType.STR_LAYOUT)).isTrue();
  }

  @Test
  void snapshotHelpersPickTheRightRows() {
    RiskRules.Rule notIn =
        new RiskRules.Rule(
            null, 10, "C", RiskAttribute.NATIONALITY, RuleOperator.NOT_IN, Set.of("PH"));
    assertThat(notIn.test(List.of("US"))).isTrue();
    assertThat(notIn.test(List.of("PH"))).isFalse();
    SlaMatrix sla =
        new SlaMatrix(
            REF,
            List.of(
                new SlaMatrix.Rule(
                    null, "INVESTIGATION", null, null, 72, 24, "R", SlaCalendar.CALENDAR),
                new SlaMatrix.Rule(
                    null, "INVESTIGATION", "PEP", null, 24, 8, "R", SlaCalendar.CALENDAR),
                new SlaMatrix.Rule(
                    null, "INVESTIGATION", "PEP", "HIGH", 12, 4, "R", SlaCalendar.CALENDAR)));
    assertThat(sla.ruleFor("INVESTIGATION", "PEP", "HIGH").orElseThrow().slaHours()).isEqualTo(12);
    assertThat(sla.ruleFor("INVESTIGATION", "PEP", "LOW").orElseThrow().slaHours()).isEqualTo(24);
    assertThat(sla.ruleFor("INVESTIGATION", "EDD", null).orElseThrow().slaHours()).isEqualTo(72);
    assertThat(sla.ruleFor("RETURNED", "EDD", null)).isEmpty();
  }

  @Test
  void listRecordsAreReadWithTheFrsMessages() {
    List<String> errors = new ArrayList<>();
    ListRecord ok =
        ListRecord.read(
            2,
            Map.of(
                ListRecord.REFERENCE, "R1",
                ListRecord.PRIMARY_NAME, "Invented Name",
                ListRecord.ENTITY_TYPE, "entity",
                ListRecord.ALIASES, "A1; ;A2",
                ListRecord.BIRTH_DATE, "04/03/1971",
                ListRecord.LIST_TYPE, "pep"),
            "SANCTION",
            errors);
    assertThat(errors).isEmpty();
    assertThat(ok.values().aliases()).hasSize(2);
    assertThat(ok.values().birthDate()).isEqualTo(LocalDate.of(1971, 3, 4));
    assertThat(ok.values().listType()).isEqualTo("PEP");
    assertThat(
            ListRecord.read(
                7,
                Map.of(ListRecord.BIRTH_DATE, "31-02-1970", ListRecord.ENTITY_TYPE, "X"),
                "PEP",
                errors))
        .isNull();
    assertThat(errors)
        .containsExactly(
            "Line 7: reference is missing",
            "Line 7: name is missing",
            "Line 7: entity type must be INDIVIDUAL or ENTITY",
            "Line 7: birth date is not a valid date");
    assertThat(ListRecord.raw(Map.of(ListRecord.REFERENCE, "R1"))).startsWith("R1,");
  }
}
