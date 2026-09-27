package com.iortatechnxt.brokerverse.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.common.storage.FileStore;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.service.FileContentMigrationJob;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Build step ST1: {@code FILE_BYTEA_MIGRATION} copies the files still kept in {@code bytea} columns
 * to the file store with the SHA-256 checked on every row, sets {@code stored_file_id}, leaves a
 * row whose content does not match its recorded checksum, and changes nothing on a second run.
 * Until a row is copied its download still serves the bytes from the database.
 */
@IntegrationTest
class FileContentMigrationIT {

  private static final byte[] PDF =
      "%PDF-1.4\nlegacy attachment\n%%EOF".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] CSV = "item\nlegacy extract\n".getBytes(StandardCharsets.UTF_8);
  private static final byte[] REPORT = "code\nlegacy report\n".getBytes(StandardCharsets.UTF_8);
  private static final byte[] MAIL = "%PDF-1.4\nmail\n%%EOF".getBytes(StandardCharsets.US_ASCII);

  @Autowired private FileContentMigrationJob job;
  @Autowired private StoredFileService files;
  @Autowired private FileStore store;
  @Autowired private AttachmentService attachments;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private MockMvc mvc;
  @Autowired private AsUser as;

  @Test
  void copiesEachRowOnceWithItsChecksumChecked() throws Exception {
    String tag = UUID.randomUUID().toString().substring(0, 8);
    Long company =
        jdbc.queryForObject("select id from org_company order by id limit 1", Long.class);
    long attachment = legacyAttachment(tag);
    long extract = legacyExtract(company, tag, CSV, Sha256.hex(CSV));
    long corrupt = legacyExtract(company, tag + "-bad", CSV, Sha256.hex(PDF));
    long run = legacyReportRun();
    long mail = legacyMailAttachment(company);

    // Before the copy the download serves the bytes kept in the database.
    mvc.perform(
            get("/api/v1/attachments/" + attachment + "/content")
                .with(
                    user("accountant").authorities(new SimpleGrantedAuthority("ATTACHMENT_VIEW"))))
        .andExpect(status().isOk())
        .andExpect(content().bytes(PDF));

    JobOutcome first = job.execute(BusinessClock.today(Clock.systemUTC()));
    assertThat(first.itemsProcessed()).isGreaterThanOrEqualTo(4);
    assertThat(first.message()).contains("ops_extract_file").contains("1 failed");

    StoredFile copied = stored("doc_attachment", attachment);
    assertThat(copied.getOwnerEntityType()).isEqualTo(AttachmentService.OWNER_TYPE);
    assertThat(copied.getOwnerEntityId()).isEqualTo(String.valueOf(attachment));
    assertThat(copied.getSha256()).isEqualTo(Sha256.hex(PDF));
    assertThat(store.get(copied.objectRef())).isEqualTo(PDF);
    assertThat(as.run("accountant", () -> attachments.download(attachment).content()))
        .isEqualTo(PDF);
    assertThat(store.get(stored("ops_extract_file", extract).objectRef())).isEqualTo(CSV);
    assertThat(stored("report_run", run).getRecordClass()).isEqualTo("REPORT_OUTPUT");
    StoredFile mailFile = stored("msg_outbound_attachment", mail);
    assertThat(mailFile.getRecordClass()).isEqualTo("GENERAL_DOCUMENT");
    assertThat(store.get(mailFile.objectRef())).isEqualTo(MAIL);
    assertThat(storedFileId("ops_extract_file", corrupt)).isNull();

    // After the copy the download answers with the presigned link.
    mvc.perform(
            get("/api/v1/attachments/" + attachment + "/content")
                .with(
                    user("accountant").authorities(new SimpleGrantedAuthority("ATTACHMENT_VIEW"))))
        .andExpect(status().isFound())
        .andExpect(header().string("Cache-Control", "no-store"));

    Integer before = jdbc.queryForObject("select count(*) from stored_file", Integer.class);
    JobOutcome second = job.execute(BusinessClock.today(Clock.systemUTC()));
    assertThat(second.itemsProcessed()).isZero();
    assertThat(jdbc.queryForObject("select count(*) from stored_file", Integer.class))
        .isEqualTo(before);
    assertThat(stored("doc_attachment", attachment).getId()).isEqualTo(copied.getId());

    mvc.perform(
            get("/api/v1/files/content-migration")
                .with(user("admin").authorities(new SimpleGrantedAuthority("SYSTEM_MONITOR"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.table == 'ops_extract_file')].remaining").value(1))
        .andExpect(jsonPath("$[?(@.table == 'doc_attachment_content')].remaining").value(0))
        .andExpect(jsonPath("$[?(@.table == 'clx_billing_document')].total").value(0));
    mvc.perform(
            get("/api/v1/files/content-migration")
                .with(user("accountant").authorities(new SimpleGrantedAuthority("REPORT_VIEW"))))
        .andExpect(status().isForbidden());
  }

  private StoredFile stored(String table, long id) {
    return files.get(storedFileId(table, id));
  }

  private Long storedFileId(String table, long id) {
    return jdbc.queryForObject(
        "select stored_file_id from " + table + " where id = ?", Long.class, id);
  }

  private long legacyAttachment(String tag) {
    Long id =
        jdbc.queryForObject(
            "insert into doc_attachment (entity_type, entity_id, file_name, content_type,"
                + " size_bytes, sha256, created_at, created_by)"
                + " values ('JournalBatch', ?, 'legacy.pdf', 'application/pdf', ?, ?, now(),"
                + " 'accountant') returning id",
            Long.class,
            "LEGACY-" + tag,
            PDF.length,
            Sha256.hex(PDF));
    jdbc.update(
        "insert into doc_attachment_content (attachment_id, content) values (?, ?)", id, PDF);
    return id;
  }

  private long legacyExtract(Long company, String tag, byte[] bytes, String sha256) {
    return jdbc.queryForObject(
        "insert into ops_extract_file (company_id, folder, file_name, content_type, size_bytes,"
            + " sha256, content, source_module, created_at, created_by)"
            + " values (?, ?, 'legacy.csv', 'text/csv', ?, ?, ?, 'REMITTANCE', now(), 'SYSTEM')"
            + " returning id",
        Long.class,
        company,
        "LEGACY/" + tag,
        bytes.length,
        sha256,
        bytes);
  }

  private long legacyReportRun() {
    Long id =
        jdbc.queryForObject(
            "insert into report_run (report_code, title, category, action, format, row_count,"
                + " file_name, content_type, size_bytes, created_at, created_by)"
                + " values ('ST1-LEGACY', 'Legacy', 'OPERATIONS', 'EXPORT', 'CSV', 1,"
                + " 'legacy.csv', 'text/csv', ?, now(), 'cashier') returning id",
            Long.class,
            REPORT.length);
    jdbc.update("insert into report_run_file (run_id, content) values (?, ?)", id, REPORT);
    return id;
  }

  private long legacyMailAttachment(Long company) {
    Long message =
        jdbc.queryForObject(
            "insert into msg_outbound (company_id, purpose, recipients, subject, body, status,"
                + " created_at, created_by) values (?, 'LEGACY', 'a@example.com', 'Legacy', 'Body',"
                + " 'SENT', now(), 'SYSTEM') returning id",
            Long.class,
            company);
    return jdbc.queryForObject(
        "insert into msg_outbound_attachment (message_id, file_name, mime_type, size_bytes,"
            + " sha256, protected, content) values (?, 'legacy.pdf', 'application/pdf', ?, ?,"
            + " false, ?) returning id",
        Long.class,
        message,
        MAIL.length,
        Sha256.hex(MAIL),
        MAIL);
  }
}
