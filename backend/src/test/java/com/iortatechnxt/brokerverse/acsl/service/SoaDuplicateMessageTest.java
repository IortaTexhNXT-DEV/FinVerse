package com.iortatechnxt.brokerverse.acsl.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The refusal of a duplicate insurer statement names the insurer, not its party code. */
class SoaDuplicateMessageTest {

  @Test
  void theRefusalNamesTheInsurer() {
    assertThat(
            SoaUploadService.duplicateMessage("Mabuhay General Insurance Corp.", "SOA-2026-000004"))
        .isEqualTo(
            "This file was already uploaded for Mabuhay General Insurance Corp. as"
                + " SOA-2026-000004");
  }
}
