package com.iortatechnxt.brokerverse.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** The rule parameters of the incentive criteria, checked in business words. */
class IncentiveRuleParametersTest {

  private final IncentiveRuleParameters parameters =
      new IncentiveRuleParameters(new ObjectMapper());

  @Test
  void everyTypeTakesTheMinimumGrossPremium() {
    assertThat(parameters.allowed("CAMPAIGN"))
        .extracting(IncentiveRuleParameters.Parameter::label)
        .containsExactly("Minimum Gross Premium");
    assertThat(parameters.allowed(null)).hasSize(1);
  }

  @Test
  void theParametersAreCheckedInBusinessWords() {
    assertThatCode(() -> parameters.check("CAMPAIGN", "{\"minimumPremium\": 5000}"))
        .doesNotThrowAnyException();
    assertThatCode(() -> parameters.check("CAMPAIGN", " ")).doesNotThrowAnyException();
    assertThatCode(() -> parameters.check("CAMPAIGN", null)).doesNotThrowAnyException();
    assertThatThrownBy(() -> parameters.check("CAMPAIGN", "{\"minimumPremium\": \"abc\"}"))
        .hasMessage("Minimum Gross Premium must be a number")
        .extracting("code")
        .isEqualTo("INCENTIVE_RULE_PARAMS_INVALID");
    assertThatThrownBy(() -> parameters.check("CAMPAIGN", "{\"minimumPremium\": \"\"}"))
        .hasMessage("Enter a value for each parameter");
    assertThatThrownBy(() -> parameters.check("CAMPAIGN", "{\"minimumPremium\": -1}"))
        .hasMessage("Minimum Gross Premium cannot be negative");
    assertThatThrownBy(() -> parameters.check("CAMPAIGN", "{\"bonus\": 1}"))
        .hasMessage("bonus is not a parameter of this incentive type");
    assertThatThrownBy(() -> parameters.check("CAMPAIGN", "[1]"))
        .hasMessage("Enter the rule parameters as parameter and value rows");
    assertThatThrownBy(() -> parameters.check("CAMPAIGN", "{oops"))
        .hasMessage("Enter the rule parameters as parameter and value rows");
  }
}
