package com.iortatechnxt.brokerverse.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.common.storage.FileStore;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import com.iortatechnxt.brokerverse.opsledger.service.ExtractRepositoryService;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DropContent;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DroppedFile;
import com.iortatechnxt.brokerverse.report.TestArchivedReport;
import com.iortatechnxt.brokerverse.report.core.ReportArchiveService;
import com.iortatechnxt.brokerverse.report.domain.ReportRun;
import com.iortatechnxt.brokerverse.report.domain.ReportRun.RunFile;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.domain.ScanStatus;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Build step ST1: every module that keeps files stores them through the file store and decides,
 * through its {@code FileOwnerAccess}, who may open them with a presigned link; users without the
 * module's permission are refused.
 */
@IntegrationTest
class ModuleFileAccessIT {

  private static final byte[] PDF =
      "%PDF-1.4\n1 0 obj << >> endobj\ntrailer\n%%EOF".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] CSV = "item\nOne\n".getBytes(StandardCharsets.UTF_8);

  @Autowired private MockMvc mvc;
  @Autowired private StoredFileService files;
  @Autowired private FileStore store;
  @Autowired private AttachmentService attachments;
  @Autowired private ExtractRepositoryService extracts;
  @Autowired private ReportArchiveService archive;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private ResultActions link(String username, Long fileId, String... authorities) throws Exception {
    List<SimpleGrantedAuthority> granted =
        Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
    return mvc.perform(
        get("/api/v1/files/" + fileId + "/link").with(user(username).authorities(granted)));
  }

  private StoredFile generated(String ownerType, String ownerId) {
    return files.storeChecked(
        new StoreRequest(
            new FileOwner(null, ownerType, ownerId),
            null,
            "GENERAL_DOCUMENT",
            "document.pdf",
            PDF,
            null),
        "application/pdf",
        FileOrigin.GENERATED);
  }

  @ParameterizedTest
  @CsvSource({
    "OutboundMessage, MESSAGE_VIEW",
    "PrintBatch, CASH_PRINT",
    "RemittanceBatchDocument, REMIT_APPROVE",
    "PlacementSlip, ACCOUNT_VIEW",
    "InsuranceAdvice, EPOLICY_SEND",
    "EpolicyUpload, EPOLICY_MANAGE",
    "ServiceInvoice, BOOKING_ADJUST",
    "EodOutput, DISB_VIEW",
    "BillingStatement, CLX_VIEW"
  })
  void theOwningModuleDecidesWhoGetsALink(String ownerType, String permission) throws Exception {
    StoredFile file = generated(ownerType, "1");
    assertThat(file.getScanStatus()).isEqualTo(ScanStatus.CLEAN);
    assertThat(file.getScanResult()).isEqualTo(FileOrigin.GENERATED_RESULT);
    link("accountant", file.getId(), permission)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").exists());
    link("accountant", file.getId(), "REPORT_VIEW").andExpect(status().isForbidden());
  }

  @Test
  void attachmentsAreStoredWithTheirClassAndTheDocumentAccessRules() throws Exception {
    String record = "ST1-" + UUID.randomUUID().toString().substring(0, 8);
    Attachment plain =
        as.run(
            "accountant",
            () ->
                attachments.upload(
                    new AttachmentTarget("JournalBatch", record), "a.pdf", PDF, null));
    StoredFile stored = files.get(plain.getStoredFileId());
    assertThat(stored.getOwnerEntityType()).isEqualTo(AttachmentService.OWNER_TYPE);
    assertThat(stored.getOwnerEntityId()).isEqualTo(String.valueOf(plain.getId()));
    assertThat(stored.getRecordClass()).isEqualTo("GENERAL_DOCUMENT");
    assertThat(stored.getSha256()).isEqualTo(plain.getSha256());
    assertThat(store.get(stored.objectRef())).isEqualTo(PDF);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from doc_attachment_content where attachment_id = ?",
                Integer.class,
                plain.getId()))
        .isZero();
    assertThat(as.run("accountant", () -> attachments.download(plain.getId()).content()))
        .isEqualTo(PDF);
    link("accountant", stored.getId(), "ATTACHMENT_VIEW").andExpect(status().isOk());
    link("accountant", stored.getId(), "ATTACHMENT_MANAGE").andExpect(status().isForbidden());

    Attachment receipt =
        as.run(
            "accountant",
            () ->
                attachments.upload(
                    new AttachmentTarget("JournalBatch", record),
                    "or.pdf",
                    PDF,
                    null,
                    "OFFICIAL_RECEIPT"));
    assertThat(files.get(receipt.getStoredFileId()).getRecordClass()).isEqualTo("OFFICIAL_RECEIPT");
  }

  @Test
  void extractFilesOpenToOperationsAndTheModuleThatProducedThem() throws Exception {
    Long company =
        jdbc.queryForObject("select id from org_company order by id limit 1", Long.class);
    String folder = "ST1/" + UUID.randomUUID().toString().substring(0, 8);
    DroppedFile dropped =
        as.run(
            "accountant",
            () ->
                extracts.store(
                    company,
                    new ExtractFile.Location(folder, "billing.csv"),
                    new DropContent("text/csv", CSV),
                    new ExtractFile.Origin("COMMISSION", "B-1")));
    Long stored =
        jdbc.queryForObject(
            "select stored_file_id from ops_extract_file where id = ?", Long.class, dropped.id());
    assertThat(files.get(stored).getSha256()).isEqualTo(dropped.sha256());
    assertThat(as.run("accountant", () -> extracts.content(dropped.id()))).isEqualTo(CSV);
    link("accountant", stored, "OPS_VIEW").andExpect(status().isOk());
    link("accountant", stored, "COMMREC_PROCESS").andExpect(status().isOk());
    link("accountant", stored, "RECON_PROCESS").andExpect(status().isForbidden());
  }

  @Test
  void reportFilesNeedTheExportPermissionAndBatchesTheirCreator() throws Exception {
    ReportRun run =
        as.run(
            "admin",
            () ->
                archive.archiveGenerated(
                    TestArchivedReport.CODE,
                    List.of("note"),
                    1,
                    new RunFile("CSV", "report.csv", "text/csv", CSV),
                    null));
    StoredFile runFile = files.get(run.getStoredFileId());
    assertThat(runFile.getRecordClass()).isEqualTo(ReportArchiveService.REPORT_OUTPUT);
    assertThat(runFile.getBucketClass().name()).isEqualTo("REPORTS");
    link("cashier", runFile.getId(), "OPS_REPORT_EXPORT").andExpect(status().isOk());
    link("cashier", runFile.getId(), "OPS_REPORT_VIEW").andExpect(status().isForbidden());

    ReportRun str =
        as.run(
            "admin",
            () ->
                archive.archiveGenerated(
                    TestArchivedReport.CODE,
                    List.of("note"),
                    1,
                    new RunFile("CSV", "str.csv", "text/csv", CSV),
                    null,
                    "STR"));
    assertThat(files.get(str.getStoredFileId()).getRecordClass()).isEqualTo("STR");

    Long batch =
        jdbc.queryForObject(
            "insert into report_batch (batch_no, format, status, created_at, created_by)"
                + " values (?, 'PDF', 'COMPLETED', now(), 'fmanager') returning id",
            Long.class,
            "RB-ST1-" + UUID.randomUUID().toString().substring(0, 8));
    StoredFile batchFile = generated("ReportBatch", String.valueOf(batch));
    link("fmanager", batchFile.getId(), "REPORT_VIEW").andExpect(status().isOk());
    link("gltl", batchFile.getId(), "REPORT_VIEW").andExpect(status().isForbidden());
  }
}
