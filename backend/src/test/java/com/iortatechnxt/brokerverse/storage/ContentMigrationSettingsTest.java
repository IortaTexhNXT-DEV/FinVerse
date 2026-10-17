package com.iortatechnxt.brokerverse.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.attachment.service.AttachmentRecordClasses;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.ContentMigrationProperties;
import org.junit.jupiter.api.Test;

/** Settings of the copy job, numeric owner keys and the record classes of attachments. */
class ContentMigrationSettingsTest {

  @Test
  void theCopyIsManualWithBoundedBatchesByDefault() {
    ContentMigrationProperties defaults = new ContentMigrationProperties(null, null);
    assertThat(defaults.cron()).isEqualTo("-");
    assertThat(defaults.batchSize()).isEqualTo(20);
    assertThat(new ContentMigrationProperties(" 0 0 3 * * * ", 0).batchSize()).isEqualTo(20);
    assertThat(new ContentMigrationProperties("0 0 3 * * *", 10_000).batchSize()).isEqualTo(500);
    assertThat(new ContentMigrationProperties(" 0 0 3 * * * ", 50).cron()).isEqualTo("0 0 3 * * *");
  }

  @Test
  void ownerKeysAreNumericOnlyWhenTheyAreIds() {
    assertThat(new FileOwner(null, "PrintBatch", "42").numericId()).contains(42L);
    assertThat(new FileOwner(null, "ServiceInvoice", "SI-HO-2026-1").numericId()).isEmpty();
    assertThat(new FileOwner(null, "PrintBatch", "").numericId()).isEmpty();
    assertThat(new FileOwner(null, "PrintBatch", null).numericId()).isEmpty();
    assertThat(new FileOwner(null, "PrintBatch", "1".repeat(19)).numericId()).isEmpty();
  }

  @Test
  void attachmentsTakeTheClassOfTheirDocumentType() {
    assertThat(AttachmentRecordClasses.of(null)).isEqualTo(AttachmentRecordClasses.GENERAL);
    assertThat(AttachmentRecordClasses.of("VALID_ID")).isEqualTo("GENERAL_DOCUMENT");
    assertThat(AttachmentRecordClasses.of("EPOLICY")).isEqualTo("POLICY_DOCUMENT");
    assertThat(AttachmentRecordClasses.of("OFFICIAL_RECEIPT")).isEqualTo("OFFICIAL_RECEIPT");
    assertThat(AttachmentRecordClasses.of("RELEASE_PAPERS")).isEqualTo("CLAIM_SETTLEMENT");
  }
}
