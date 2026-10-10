package com.iortatechnxt.brokerverse.cashiering.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The source of an unapplied payment reads in words: module name and reference, no prefixes. */
class UnappliedSourceTextTest {

  @Test
  void theModuleIsNamedAndThePaymentPrefixSpelledOut() {
    assertThat(UnappliedInquiry.source("CASHIERING", "PAY:PAY-2026-000012"))
        .isEqualTo("Cashiering payment PAY-2026-000012");
    assertThat(UnappliedInquiry.source("COMMISSION", "CPAY:41"))
        .isEqualTo("Commission Receivables commission payment 41");
    assertThat(UnappliedInquiry.source("MIGRATION", "AR-HO-000004"))
        .isEqualTo("Migration AR-HO-000004");
    assertThat(UnappliedInquiry.source(null, null)).isEmpty();
  }
}
