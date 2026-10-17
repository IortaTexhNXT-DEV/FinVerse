package com.iortatechnxt.brokerverse.eb.comparative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.eb.comparative.service.ThresholdEvaluator;
import com.iortatechnxt.brokerverse.eb.domain.EbThresholdRule;
import com.iortatechnxt.brokerverse.eb.domain.EbThresholdRuleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The value threshold rules (FR-EB-042): per line or every line, TSI or annual premium. */
class ThresholdEvaluatorTest {

  private static final LocalDate DAY = LocalDate.of(2026, 9, 26);

  private static EbThresholdRule rule(
      String line, EbThresholdRule.Measure measure, String amount, int level) {
    EbThresholdRule rule =
        new EbThresholdRule(
            1L,
            new EbThresholdRule.Data(
                line,
                measure,
                new BigDecimal(amount),
                "PHP",
                level == 1 ? "EB_THRESHOLD_APPROVE" : "EB_SETUP",
                level,
                LocalDate.of(2026, 1, 1),
                null,
                null));
    rule.authorize("checker", Instant.EPOCH);
    return rule;
  }

  @Test
  void theRulesMetGiveTheirTextAndTheHighestLevelApprover() {
    EbThresholdRuleRepository repo = mock(EbThresholdRuleRepository.class);
    when(repo.findByCompanyIdOrderByIdAsc(1L))
        .thenReturn(
            List.of(
                rule(null, EbThresholdRule.Measure.ANNUAL_PREMIUM, "20000000", 1),
                rule("GLI", EbThresholdRule.Measure.TSI, "500000000", 2)));
    ThresholdEvaluator evaluator = new ThresholdEvaluator(repo);

    ThresholdEvaluator.Result none =
        evaluator.evaluate(
            1L,
            Map.of("HMO", new ThresholdEvaluator.Measures(BigDecimal.ZERO, new BigDecimal("1000"))),
            DAY);
    assertThat(none.met()).isFalse();
    assertThat(none.text()).isNull();
    assertThat(none.approver()).isEqualTo("EB_THRESHOLD_APPROVE");

    ThresholdEvaluator.Result both =
        evaluator.evaluate(
            1L,
            Map.of(
                "GLI",
                new ThresholdEvaluator.Measures(
                    new BigDecimal("600000000"), new BigDecimal("25000000"))),
            DAY);
    assertThat(both.rules()).hasSize(2);
    assertThat(both.text()).contains("GLI TSI at or above PHP 500,000,000.00");
    assertThat(both.approver()).isEqualTo("EB_SETUP");
    assertThat(
            evaluator
                .evaluate(
                    1L,
                    Map.of("GLI", new ThresholdEvaluator.Measures(null, new BigDecimal("1"))),
                    LocalDate.of(2025, 1, 1))
                .met())
        .isFalse();
  }
}
