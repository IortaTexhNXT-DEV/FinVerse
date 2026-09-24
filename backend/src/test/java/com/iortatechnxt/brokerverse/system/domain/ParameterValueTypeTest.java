package com.iortatechnxt.brokerverse.system.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ParameterValueTypeTest {

  @Test
  void validatesEachType() {
    assertThat(ParameterValueType.STRING.validate("anything", null, null)).isEmpty();
    assertThat(ParameterValueType.INTEGER.validate("30", 5, 480)).isEmpty();
    assertThat(ParameterValueType.INTEGER.validate("3", 5, 480)).isPresent();
    assertThat(ParameterValueType.INTEGER.validate("500", 5, 480)).isPresent();
    assertThat(ParameterValueType.INTEGER.validate("abc", null, null))
        .contains("must be a whole number");
    assertThat(ParameterValueType.DECIMAL.validate("12.50", null, null)).isEmpty();
    assertThat(ParameterValueType.DECIMAL.validate("1,2", null, null)).isPresent();
    assertThat(ParameterValueType.BOOLEAN.validate("true", null, null)).isEmpty();
    assertThat(ParameterValueType.BOOLEAN.validate("yes", null, null)).isPresent();
    assertThat(ParameterValueType.INTEGER_LIST.validate("30, 60,90", 1, 3650)).isEmpty();
    assertThat(ParameterValueType.INTEGER_LIST.validate("60,30", 1, 3650))
        .contains("values must be in ascending order");
    assertThat(ParameterValueType.INTEGER_LIST.validate("", 1, 3650)).isPresent();
    assertThat(ParameterValueType.INTEGER_LIST.validate("30,x", 1, 3650)).isPresent();
    assertThat(ParameterValueType.CODE_LIST.validate("", null, null)).isEmpty();
    assertThat(ParameterValueType.CODE_LIST.validate("1606, 1607", null, null)).isEmpty();
    assertThat(ParameterValueType.CODE_LIST.validate("16 06", null, null)).isPresent();
    assertThat(ParameterValueType.items(" a, ,b ")).containsExactly("a", "b");
    assertThat(ParameterValueType.decimal(" 1.5 ")).isEqualByComparingTo("1.5");
  }
}
