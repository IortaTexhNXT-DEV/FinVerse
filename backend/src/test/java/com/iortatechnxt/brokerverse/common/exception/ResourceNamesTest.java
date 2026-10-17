package com.iortatechnxt.brokerverse.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Records are named in words in user messages, never by class name. */
class ResourceNamesTest {

  @Test
  void theReportedMessagesUseBusinessNames() {
    assertThat(new ResourceNotFoundException("AutoBookRule", 7).getMessage())
        .isEqualTo("Auto-booking rule not found: 7");
    assertThat(new ResourceNotFoundException("ServiceInvoiceType", 3).getMessage())
        .isEqualTo("Service invoice type not found: 3");
    assertThat(new DuplicateResourceException("IncentiveRule", "R1").getMessage())
        .isEqualTo("Incentive rule already exists: R1");
  }

  @Test
  void classNamesCodesAndShortFormsAreWrittenInWords() {
    assertThat(ResourceNames.of("PackageRequest work item")).isEqualTo("Package request work item");
    assertThat(ResourceNames.of("EbProgramme")).isEqualTo("Employee benefits programme");
    assertThat(ResourceNames.of("MigBatch")).isEqualTo("Migration batch");
    assertThat(ResourceNames.of("GlSlRun")).isEqualTo("GL SL run");
    assertThat(ResourceNames.of("EB_MEMBER")).isEqualTo("Employee benefits member");
    assertThat(ResourceNames.of("WorkCase")).isEqualTo("Work item");
  }

  @Test
  void namesInWordsAreKept() {
    assertThat(ResourceNames.of("GL account")).isEqualTo("GL account");
    assertThat(ResourceNames.of("Package version")).isEqualTo("Package version");
    assertThat(ResourceNames.of("Account")).isEqualTo("Account");
    assertThat(ResourceNames.of(null)).isEqualTo("Record");
  }
}
