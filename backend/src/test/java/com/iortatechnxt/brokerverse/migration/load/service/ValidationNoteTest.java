package com.iortatechnxt.brokerverse.migration.load.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** The run log entry of a batch validation shows the error rate as users read it (0.00%). */
class ValidationNoteTest {

  @Test
  void theErrorRateHasTwoDecimalsAndThePercentSign() {
    Map<String, Integer> versions = new TreeMap<>(Map.of("BRANCH", 2, "LOB", 1));

    String note = ValidationService.validatedNote(versions, new BigDecimal("0.0000"), 0, 1);

    assertThat(note)
        .isEqualTo(
            "Validated with map versions {BRANCH=2, LOB=1}; error rate 0.00%; 0 unmapped codes;"
                + " 1 client pairs to review")
        .doesNotContain("percent");
  }

  @Test
  void aRateIsRoundedToTwoDecimals() {
    assertThat(ValidationService.validatedNote(Map.of(), new BigDecimal("2.3456"), 3, 0))
        .contains("; error rate 2.35%; 3 unmapped codes;");
  }
}
