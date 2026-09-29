package com.iortatechnxt.brokerverse.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AsciiCaseTest {

  @Test
  void onlyAsciiLettersAreFolded() {
    assertThat(AsciiCase.equalsIgnoreCase("Local", "local")).isTrue();
    assertThat(AsciiCase.equalsIgnoreCase("JDoe_1", "jdoe_1")).isTrue();
    assertThat(AsciiCase.equalsIgnoreCase("admın", "admin")).isFalse();
    assertThat(AsciiCase.equalsIgnoreCase("Key", "key")).isFalse();
    assertThat(AsciiCase.equalsIgnoreCase(null, null)).isTrue();
    assertThat(AsciiCase.equalsIgnoreCase("a", null)).isFalse();
    assertThat(AsciiCase.equalsIgnoreCase("ab", "a")).isFalse();
    assertThat(AsciiCase.upper("privileged-ı")).isEqualTo("PRIVILEGED-ı");
  }
}
