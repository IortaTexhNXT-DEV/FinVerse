package com.iortatechnxt.finverse.underwriting;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.underwriting.domain.RiskValues;
import com.iortatechnxt.finverse.underwriting.domain.UnderwritingRules;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class UnderwritingRulesTest {

  private static RiskValues risk(String sumInsured) {
    return new RiskValues(
        "Warehouse",
        new BigDecimal(sumInsured),
        new BigDecimal("0.18"),
        BigDecimal.ZERO,
        null,
        null,
        null);
  }

  @Test
  void everyRiskNeedsAPositiveSumInsured() {
    assertThatCode(() -> UnderwritingRules.requirePositiveSumsInsured(List.of(risk("1000000.00"))))
        .doesNotThrowAnyException();
    assertThatThrownBy(
            () ->
                UnderwritingRules.requirePositiveSumsInsured(
                    List.of(risk("1000000.00"), risk("0.00"))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Risk 2: the sum insured must be greater than zero")
        .extracting("code")
        .isEqualTo("SUM_INSURED_NOT_POSITIVE");
    assertThatThrownBy(() -> UnderwritingRules.requirePositiveSumsInsured(List.of(risk("-5"))))
        .isInstanceOf(BusinessRuleException.class);
  }
}
