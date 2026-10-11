package com.iortatechnxt.brokerverse.bulk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJobStatus;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowStatus;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.bulk.service.BulkUpload;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

/**
 * Bulk extensions for Operations (BRQID.006, CSHID.008): TXT files, duplicate file block, outcome
 * categories and reprocessing of failed rows.
 */
@IntegrationTest
class BulkExtensionsIT {

  @Autowired private BulkService bulk;
  @Autowired private AsUser as;
  @Autowired private TestData data;
  @Autowired private Api api;

  private BulkJob upload(String handler, String fileName, String content) {
    return as.run(
        "proc",
        () ->
            bulk.upload(
                new BulkUpload(
                    data.company().getId(),
                    handler,
                    fileName,
                    content.getBytes(StandardCharsets.UTF_8),
                    null)));
  }

  private static String fixed(String reference, String amount) {
    return String.format("%-10s%10s%n", reference, amount);
  }

  @Test
  void aFixedWidthFileIsCategorisedReprocessedAndNeverUploadedTwice() throws Exception {
    String tag = UUID.randomUUID().toString().substring(0, 4);
    String file =
        fixed("A" + tag, "250.00") + fixed("B" + tag, "50.00") + fixed("RETRY" + tag, "75.00");
    BulkJob job = upload(TestPaymentFileHandler.CODE, "bills_" + tag + ".txt", file);
    assertThat(job.getValidRows()).isEqualTo(3);
    assertThat(job.getFileSha256()).hasSize(64);

    BulkJob done = as.run("proc", () -> bulk.commit(job.getId()));
    assertThat(done.getCommittedRows()).isEqualTo(2);
    assertThat(done.getFailedRows()).isEqualTo(1);
    assertThat(bulk.outcomes(job.getId()))
        .containsEntry("APPLIED", 1L)
        .containsEntry("UNAPPLIED", 1L);

    BulkJob reprocessed = as.run("proc", () -> bulk.reprocess(job.getId()));
    assertThat(reprocessed.getCommittedRows()).isEqualTo(3);
    assertThat(reprocessed.getFailedRows()).isZero();
    assertThat(reprocessed.getReprocessCount()).isEqualTo(1);
    assertThat(bulk.outcomes(job.getId())).containsEntry("UNAPPLIED", 2L);
    BulkRowRecord retried =
        bulk.rows(job.getId(), BulkRowStatus.COMMITTED, Pageable.ofSize(10)).getContent().stream()
            .filter(r -> r.getResultRef().startsWith("PAY-RETRY"))
            .findFirst()
            .orElseThrow();
    assertThat(retried.getAttempts()).isEqualTo(2);
    assertThat(retried.getOutcome()).isEqualTo("UNAPPLIED");

    try (XSSFWorkbook report =
        new XSSFWorkbook(new ByteArrayInputStream(bulk.report(job.getId())))) {
      assertThat(report.getSheet("Rows").getRow(1).getCell(4).getStringCellValue())
          .isEqualTo("APPLIED");
    }

    assertThatThrownBy(() -> upload(TestPaymentFileHandler.CODE, "again.txt", file))
        .extracting("code")
        .isEqualTo("BULK_DUPLICATE_FILE");

    api.doGet("proc", "/api/v1/bulk/jobs/" + job.getId() + "/outcomes")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.APPLIED").value(1));
    api.doPost("proc", "/api/v1/bulk/jobs/" + job.getId() + "/reprocess", null)
        .andExpect(jsonPath("$.reprocessCount").value(2));
    api.doGet("proc", "/api/v1/bulk/handlers/" + TestPaymentFileHandler.CODE)
        .andExpect(jsonPath("$.blocksDuplicateFiles").value(true))
        .andExpect(jsonPath("$.outcomeCategories[0]").value("APPLIED"));
  }

  @Test
  void aCancelledUploadDoesNotBlockTheSameFileAndOpenJobsCannotBeReprocessed() {
    String tag = UUID.randomUUID().toString().substring(0, 6);
    String file = fixed("C" + tag, "10.00");
    BulkJob first = upload(TestPaymentFileHandler.CODE, "c.txt", file);
    assertThatThrownBy(() -> as.run("proc", () -> bulk.reprocess(first.getId())))
        .extracting("code")
        .isEqualTo("BULK_JOB_NOT_COMPLETED");
    as.run("proc", () -> bulk.cancel(first.getId()));
    BulkJob second = upload(TestPaymentFileHandler.CODE, "c.txt", file);
    assertThat(second.getStatus()).isEqualTo(BulkJobStatus.VALIDATED);
  }

  @Test
  void delimitedTextFilesAreReadWithTheirHeaderLine() throws IOException {
    String txt =
        "Plate No|Amount|Inception|Fleet|Remarks\n"
            + "TXT "
            + UUID.randomUUID().toString().substring(0, 4)
            + "|1500|2026-10-01|N|from text\n";
    BulkJob job = upload(TestBulkHandler.CODE, "vehicles.txt", txt);
    assertThat(job.getValidRows()).isEqualTo(1);
    var row = bulk.rows(job.getId(), null, Pageable.ofSize(5)).getContent().get(0);
    assertThat(bulk.values(row))
        .containsEntry("Amount", "1500")
        .containsEntry("Remarks", "from text");
    assertThat(bulk.outcomes(job.getId())).isEmpty();
    as.run("proc", () -> bulk.cancel(job.getId()));

    assertThatThrownBy(() -> upload(TestBulkHandler.CODE, "bad.txt", "no separators here\n"))
        .extracting("code")
        .isEqualTo("BULK_TEXT_LAYOUT");
  }
}
