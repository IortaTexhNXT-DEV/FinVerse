package com.iortatechnxt.brokerverse.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class SafePatternTest {

  @Test
  void acceptsOnlyNonBlankValidPatterns() {
    assertThat(SafePattern.isValid("MTR\\d{2}")).isTrue();
    assertThat(SafePattern.isValid("[A-Z]{3}")).isTrue();
    assertThat(SafePattern.isValid(null)).isFalse();
    assertThat(SafePattern.isValid("  ")).isFalse();
    assertThat(SafePattern.isValid("([A-Z")).isFalse();
    // Back-references are outside RE2 syntax.
    assertThat(SafePattern.isValid("(a)\\1")).isFalse();
  }

  @Test
  void matchesTheWholeValue() {
    assertThat(SafePattern.matches("MTR\\d{2}", "MTR12")).isTrue();
    assertThat(SafePattern.matches("MTR\\d{2}", "MTR123")).isFalse();
    assertThat(SafePattern.matches("MTR\\d{2}", "XMTR12")).isFalse();
  }

  @Test
  void aCatastrophicPatternRunsInLinearTime() {
    String hostile = "a".repeat(50_000) + "!";
    long started = System.nanoTime();

    boolean matched = SafePattern.matches("(a+)+$", hostile);

    assertThat(matched).isFalse();
    assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(5));
  }
}
