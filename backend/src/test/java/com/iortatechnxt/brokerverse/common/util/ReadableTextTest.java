package com.iortatechnxt.brokerverse.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Server-built sentences with dates, amounts and status codes as users read them. */
class ReadableTextTest {

  @Test
  void writesDatesTimesAmountsAndStatusCodesForUsers() {
    assertThat(
            ReadableText.of(
                "Case SCR-2026-000005 passed its SLA in COMPLIANCE_REVIEW (due"
                    + " 2026-10-08T18:46:57.354908Z)"))
        .isEqualTo(
            "Case SCR-2026-000005 passed its SLA in compliance review (due 09-Oct-2026 02:46)");
    assertThat(
            ReadableText.of(
                "Journal INV-HO-2026-000002 of 9450000.00 reached the large journal threshold"
                    + " 1000000.00"))
        .isEqualTo(
            "Journal INV-HO-2026-000002 of 9,450,000.00 reached the large journal threshold"
                + " 1,000,000.00");
    assertThat(ReadableText.of("BCL-2026-000002: follow-up overdue since 2026-10-01."))
        .isEqualTo("BCL-2026-000002: follow-up overdue since 01-Oct-2026.");
    assertThat(ReadableText.of("due 2026-10-08T18:46Z")).isEqualTo("due 09-Oct-2026 02:46");
    assertThat(ReadableText.of("1 invoice(s) and 3 policy(s) excluded"))
        .isEqualTo("1 invoice and 3 policies excluded");
  }

  @Test
  void leavesReferencesFileNamesAndSmallNumbersAlone() {
    String text =
        "Extract BDO_EXTRACT_2026.csv of MGB-2026-000003 has 12 rows, 15.50 each, QS sent";
    assertThat(ReadableText.of(text)).isEqualTo(text);
    assertThat(ReadableText.of(null)).isNull();
    assertThat(ReadableText.of("")).isEmpty();
  }
}
