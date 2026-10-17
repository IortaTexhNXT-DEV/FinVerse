package com.iortatechnxt.brokerverse.cashiering.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.UnappliedOrigin;
import org.junit.jupiter.api.Test;

/** The history of an unapplied payment reads in words: no origin or request action codes. */
class UnappliedHistoryTextTest {

  @Test
  void intakeNamesTheOriginInWords() {
    assertThat(UnappliedHistory.receivedText(UnappliedOrigin.NO_MATCH, "Liza Manalo"))
        .isEqualTo("Unapplied payment from Liza Manalo (no match)");
    assertThat(UnappliedHistory.receivedText(UnappliedOrigin.EXCESS, null))
        .isEqualTo("Unapplied payment from unknown payor (excess)")
        .doesNotContain("_");
  }

  @Test
  void collectorRequestNamesTheActionInWords() {
    assertThat(UnappliedHistory.requestText("APPLY_TO_INVOICE", "BI-HO-2026-000008"))
        .isEqualTo("Collector request: apply to invoice BI-HO-2026-000008");
    assertThat(UnappliedHistory.requestText("REFUND", null)).isEqualTo("Collector request: refund");
  }
}
