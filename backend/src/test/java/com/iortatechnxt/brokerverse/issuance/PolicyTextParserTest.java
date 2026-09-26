package com.iortatechnxt.brokerverse.issuance;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.issuance.domain.ExtractionField;
import com.iortatechnxt.brokerverse.issuance.service.ExtractedPolicy;
import com.iortatechnxt.brokerverse.issuance.service.PolicyTextParser;
import com.iortatechnxt.brokerverse.issuance.service.PolicyTextParser.Rule;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Reading policy data from e-policy text with configurable patterns (BRNB.104). */
class PolicyTextParserTest {

  private static final String DATE = "(\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4})";
  private static final List<Rule> DEFAULTS =
      List.of(
          new Rule(
              ExtractionField.POLICY_NUMBER,
              "(?i)policy\\s*(?:no\\.?|number|#)\\s*[:#]?\\s*([A-Z0-9][A-Z0-9/-]{3,39})",
              null),
          new Rule(
              ExtractionField.PERIOD_FROM,
              "(?i)period\\s+of\\s+insurance\\s*:?\\s*(?:from\\s+)?" + DATE,
              "MM/dd/yyyy"),
          new Rule(
              ExtractionField.PERIOD_TO,
              "(?i)period\\s+of\\s+insurance\\s*:?\\s*(?:from\\s+)?(?:\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4})"
                  + "\\s*(?:to|-)\\s*"
                  + DATE,
              "MM/dd/yyyy"),
          new Rule(
              ExtractionField.PREMIUM,
              "(?i)(?:total|gross)\\s+premium\\s*:?\\s*(?:PHP|P)?\\s*([0-9][0-9,]*\\.[0-9]{2})",
              null));

  @Test
  void readsEveryFieldAndEveryPolicyNumberOfAMultiYearPolicy() {
    ExtractedPolicy found =
        PolicyTextParser.parse(
            "E-POLICY for ARN-2026-000123\nPolicy No: MGIC-1\nPolicy No: MGIC-2\nPolicy No: mgic-1\n"
                + "Period of insurance: from 10/01/2026 to 10/01/2028\nTotal premium: PHP 1,234.50",
            DEFAULTS);
    assertThat(found.policyNumbers()).containsExactly("MGIC-1", "MGIC-2");
    assertThat(found.periodFrom()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(found.periodTo()).isEqualTo(LocalDate.of(2028, 10, 1));
    assertThat(found.premium()).isEqualByComparingTo("1234.50");
    assertThat(found.arns()).containsExactly("ARN-2026-000123");
    assertThat(found.note()).isNull();
  }

  @Test
  void insurerPatternsComeFirstAndMissingValuesAreExplained() {
    List<Rule> rules =
        List.of(
            new Rule(ExtractionField.POLICY_NUMBER, "LAC/([0-9]+)", null),
            new Rule(ExtractionField.POLICY_NUMBER, "([", null),
            new Rule(ExtractionField.PERIOD_FROM, "Start (\\S+)", "dd.MM.yyyy"),
            new Rule(ExtractionField.PERIOD_TO, "End (\\S+)", null),
            new Rule(ExtractionField.PREMIUM, "Premium (\\S+)", null));
    ExtractedPolicy found =
        PolicyTextParser.parse("Ref LAC/778 Start 01.10.2026 End 2027-99-99 Premium abc", rules);
    assertThat(found.policyNumbers()).containsExactly("778");
    assertThat(found.periodFrom()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(found.periodTo()).isNull();
    assertThat(found.premium()).isNull();
    assertThat(found.note()).contains("period end not found", "premium 'abc' not understood");
    ExtractedPolicy nothing = PolicyTextParser.parse("nothing here", List.of(rules.get(1)));
    assertThat(nothing.note()).contains("invalid POLICY_NUMBER pattern", "policy number not found");
    assertThat(PolicyTextParser.arns("arn-2026-000001_and_ARN-2026-000001.pdf"))
        .containsExactly("ARN-2026-000001");
    assertThat(ExtractedPolicy.NONE.policyNumbers()).isEmpty();
  }
}
