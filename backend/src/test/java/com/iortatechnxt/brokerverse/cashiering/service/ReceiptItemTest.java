package com.iortatechnxt.brokerverse.cashiering.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The item of an official receipt line names its invoice once. */
class ReceiptItemTest {

  @Test
  void aDescriptionThatNamesTheInvoiceIsNotPrefixedWithItAgain() {
    assertThat(ReceiptDocument.item("BI-HO-2026-000001", "Commission BI-HO-2026-000001"))
        .isEqualTo("Commission BI-HO-2026-000001");
  }

  @Test
  void otherwiseTheInvoiceComesFirst() {
    assertThat(ReceiptDocument.item("BI-HO-2026-000001", "Premium"))
        .isEqualTo("BI-HO-2026-000001 Premium");
    assertThat(ReceiptDocument.item(null, "Risk management consultancy fee"))
        .isEqualTo("Risk management consultancy fee");
    assertThat(ReceiptDocument.item("BI-HO-2026-000001", null)).isEqualTo("BI-HO-2026-000001");
  }
}
