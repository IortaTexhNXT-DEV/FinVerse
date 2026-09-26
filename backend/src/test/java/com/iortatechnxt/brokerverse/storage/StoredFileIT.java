package com.iortatechnxt.brokerverse.storage;

import static com.iortatechnxt.brokerverse.support.CsrfRequests.delete;
import static com.iortatechnxt.brokerverse.support.CsrfRequests.multipart;
import static com.iortatechnxt.brokerverse.support.CsrfRequests.put;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.storage.FileStore;
import com.iortatechnxt.brokerverse.common.storage.LocalFileStore;
import com.iortatechnxt.brokerverse.common.storage.ObjectKeys;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.domain.StoredFileRepository;
import com.iortatechnxt.brokerverse.storage.service.FileLinkService;
import com.iortatechnxt.brokerverse.storage.service.FileScanResultsJob;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.StorageTestOwnerAccess;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Document storage (ST0) through the API with the local file store: upload, download link and
 * download, permission refused, checksum and type checks, integrity re-check, inbound presigned
 * upload with pending and quarantined scans, record classes and the retention mapping.
 */
@IntegrationTest
class StoredFileIT {

  private static final String TYPE = StorageTestOwnerAccess.TYPE;
  private static final byte[] PDF =
      "%PDF-1.4\n1 0 obj << >> endobj\ntrailer\n%%EOF".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
  private static final byte[] CSV =
      "date,amount\n2026-09-26,100.00\n".getBytes(StandardCharsets.UTF_8);

  @Autowired private MockMvc mvc;
  @Autowired private Api api;
  @Autowired private AsUser as;
  @Autowired private UserDetailsService users;
  @Autowired private ObjectMapper json;
  @Autowired private StoredFileService files;
  @Autowired private StoredFileRepository repository;
  @Autowired private FileStore store;
  @Autowired private FileScanResultsJob scanJob;
  @Autowired private SystemParameterService parameters;
  @Autowired private JdbcTemplate jdbc;

  private MockHttpServletRequestBuilder as(String username, MockHttpServletRequestBuilder b) {
    return b.with(user(users.loadUserByUsername(username)));
  }

  private StoredFile storeAs(String username, String ownerId, String recordClass, byte[] bytes) {
    return as.run(
        username,
        () ->
            files.store(
                new StoreRequest(
                    new FileOwner(null, TYPE, ownerId),
                    "SUPPORTING",
                    recordClass,
                    "letter.pdf",
                    bytes,
                    null)));
  }

  private static LocalDate today() {
    return LocalDate.now(ZoneOffset.UTC);
  }

  @Test
  void uploadLinkDownloadAndDelete() throws Exception {
    String owner = "ST-" + UUID.randomUUID().toString().substring(0, 8);
    MockMultipartFile file =
        new MockMultipartFile("file", "C:\\docs\\Offer.pdf", "text/plain", PDF);
    JsonNode saved =
        json.readTree(
            mvc.perform(
                    as(
                        "accountant",
                        multipart("/api/v1/files")
                            .file(file)
                            .param("ownerType", TYPE)
                            .param("ownerId", owner)
                            .param("recordClass", "GENERAL_DOCUMENT")
                            .param("documentType", "SUPPORTING")
                            .param("sha256", Sha256.hex(PDF).toUpperCase())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("Offer.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.sha256").value(Sha256.hex(PDF)))
                .andExpect(jsonPath("$.scanStatus").value("CLEAN"))
                .andExpect(jsonPath("$.retentionUntil").value(today().plusYears(20).toString()))
                .andExpect(jsonPath("$.ecmStatus").value("NOT_REQUIRED"))
                .andReturn()
                .getResponse()
                .getContentAsString());
    long id = saved.get("id").asLong();
    StoredFile row = repository.findById(id).orElseThrow();
    assertThat(row.getObjectKey())
        .matches("shared/storage-test-record/\\d{4}/\\d{2}/[0-9a-f-]{36}");
    assertThat(row.getObjectKey()).doesNotContain("offer");
    assertThat(store.get(row.objectRef())).isEqualTo(PDF);

    api.doGet("accountant", "/api/v1/files?ownerType=" + TYPE + "&ownerId=" + owner)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    api.doGet("checker", "/api/v1/files/" + id).andExpect(status().isOk());

    JsonNode link =
        api.read(
            api.doGet("checker", "/api/v1/files/" + id + "/link")
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.fileName").value("Offer.pdf")));
    String url = link.get("url").asText();
    assertThat(url).startsWith(LocalFileStore.CONTENT_PATH + "?token=");
    Instant expires = Instant.parse(link.get("expiresAt").asText());
    assertThat(expires).isBetween(Instant.now().plusSeconds(280), Instant.now().plusSeconds(301));

    mvc.perform(as("checker", get(url)))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    org.hamcrest.Matchers.startsWith("attachment; filename=\"Offer.pdf\"")))
        .andExpect(content().bytes(PDF));
    mvc.perform(as("checker", get(LocalFileStore.CONTENT_PATH + "?token=forged.token")))
        .andExpect(status().isForbidden());

    Integer issued =
        jdbc.queryForObject(
            "select count(*) from audit_log where entity_type = 'StoredFile' and entity_id = ?"
                + " and action = 'EXPORT' and username = 'checker'"
                + " and summary like 'Download link issued (valid 300 s) from %'",
            Integer.class, String.valueOf(id));
    assertThat(issued).isEqualTo(1);

    mvc.perform(as("accountant", delete("/api/v1/files/" + id))).andExpect(status().isNoContent());
    api.doGet("accountant", "/api/v1/files/" + id).andExpect(status().isNotFound());
    assertThat(repository.findById(id).orElseThrow().getDeletedBy()).isEqualTo("accountant");
  }

  @Test
  void theOwnersPermissionDecidesAndRefusalsAreAudited() throws Exception {
    StoredFile locked =
        storeAs("accountant", StorageTestOwnerAccess.LOCKED_PREFIX + "1", "GENERAL_DOCUMENT", PDF);
    api.doGet("accountant", "/api/v1/files/" + locked.getId() + "/link")
        .andExpect(status().isForbidden());
    api.doGet("accountant", "/api/v1/files/" + locked.getId()).andExpect(status().isForbidden());
    Integer refused =
        jdbc.queryForObject(
            "select count(*) from audit_log where entity_type = 'StoredFile' and entity_id = ?"
                + " and action = 'REJECT' and summary like 'Download link refused%'",
            Integer.class, String.valueOf(locked.getId()));
    assertThat(refused).isEqualTo(1);

    MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", PDF);
    mvc.perform(
            as(
                "accountant",
                multipart("/api/v1/files")
                    .file(file)
                    .param("ownerType", TYPE)
                    .param("ownerId", StorageTestOwnerAccess.LOCKED_PREFIX + "2")
                    .param("recordClass", "GENERAL_DOCUMENT")))
        .andExpect(status().isForbidden());
    mvc.perform(
            as(
                "accountant",
                multipart("/api/v1/files")
                    .file(file)
                    .param("ownerType", "UnknownRecord")
                    .param("ownerId", "1")
                    .param("recordClass", "GENERAL_DOCUMENT")))
        .andExpect(status().isForbidden());
    mvc.perform(as("accountant", delete("/api/v1/files/" + locked.getId())))
        .andExpect(status().isForbidden());
  }

  @Test
  void checksumTypeSizeAndIntegrityChecks() throws Exception {
    MockMultipartFile pdf = new MockMultipartFile("file", "a.pdf", "application/pdf", PDF);
    mvc.perform(
            as(
                "accountant",
                multipart("/api/v1/files")
                    .file(pdf)
                    .param("ownerType", TYPE)
                    .param("ownerId", "ST-CHK")
                    .param("recordClass", "GENERAL_DOCUMENT")
                    .param("sha256", Sha256.hex("something else"))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("FILE_CHECKSUM_MISMATCH"));
    assertThatThrownBy(() -> storeAs("accountant", "ST-CHK", "GENERAL_DOCUMENT", PNG))
        .extracting("code")
        .isEqualTo("FILE_CONTENT_MISMATCH");
    assertThatThrownBy(
            () ->
                files.store(
                    new StoreRequest(
                        new FileOwner(null, TYPE, "ST-CHK"),
                        null,
                        "GENERAL_DOCUMENT",
                        "run.exe",
                        PDF,
                        null)))
        .extracting("code")
        .isEqualTo("FILE_TYPE_NOT_ALLOWED");
    assertThatThrownBy(() -> storeAs("accountant", "ST-CHK", "GENERAL_DOCUMENT", new byte[0]))
        .extracting("code")
        .isEqualTo("FILE_EMPTY");
    assertThatThrownBy(() -> storeAs("accountant", "ST-CHK", "NO_SUCH_CLASS", PDF))
        .hasMessageContaining("NO_SUCH_CLASS");
    assertThatThrownBy(
            () ->
                files.store(
                    new StoreRequest(
                        new FileOwner(null, "bad type!", "1"),
                        null,
                        "GENERAL_DOCUMENT",
                        "a.pdf",
                        PDF,
                        null)))
        .extracting("code")
        .isEqualTo("INVALID_FILE_OWNER");

    StoredFile saved = storeAs("accountant", "ST-CHK", "GENERAL_DOCUMENT", PDF);
    assertThat(as.run("accountant", () -> files.read(saved.getId()))).isEqualTo(PDF);
    byte[] altered = "%PDF-1.4 altered".getBytes(StandardCharsets.US_ASCII);
    store.put(saved.objectRef(), altered, "application/pdf", Sha256.hex(altered));
    assertThatThrownBy(() -> as.run("accountant", () -> files.read(saved.getId())))
        .isInstanceOf(BusinessRuleException.class)
        .extracting("code")
        .isEqualTo("FILE_INTEGRITY_FAILURE");
  }

  @Test
  void theLinkValidityFollowsTheParameter() throws Exception {
    StoredFile saved = storeAs("accountant", "ST-TTL", "GENERAL_DOCUMENT", PDF);
    parameters.update(FileLinkService.TTL_PARAMETER, "60");
    try {
      JsonNode link = api.read(api.doGet("accountant", "/api/v1/files/" + saved.getId() + "/link"));
      assertThat(Instant.parse(link.get("expiresAt").asText()))
          .isBefore(Instant.now().plusSeconds(61));
    } finally {
      parameters.update(FileLinkService.TTL_PARAMETER, "300");
    }
  }

  private JsonNode startInbound(String owner, byte[] bytes) throws Exception {
    return api.read(
        api.doPost(
                "accountant",
                "/api/v1/files/inbound-uploads",
                Map.of(
                    "ownerType",
                    TYPE,
                    "ownerId",
                    owner,
                    "recordClass",
                    "INBOUND_FILE",
                    "fileName",
                    "bank-file.csv",
                    "sizeBytes",
                    bytes.length,
                    "sha256",
                    Sha256.hex(bytes)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.method").value("PUT")));
  }

  private StoredFile uploadInbound(JsonNode link, byte[] bytes) throws Exception {
    mvc.perform(
            as(
                "accountant",
                put(link.get("url").asText()).contentType(MediaType.TEXT_PLAIN).content(bytes)))
        .andExpect(status().isOk());
    return repository.findById(link.get("fileId").asLong()).orElseThrow();
  }

  @Test
  void inboundUploadWaitsForTheScanAndOnlyCleanFilesAreDownloadable() throws Exception {
    JsonNode link = startInbound("ST-IN-1", CSV);
    StoredFile announced = repository.findById(link.get("fileId").asLong()).orElseThrow();
    assertThat(announced.getScanStatus().name()).isEqualTo("AWAITING_UPLOAD");
    assertThat(announced.getObjectKey()).startsWith(ObjectKeys.INCOMING_PREFIX);
    api.doPost("accountant", "/api/v1/files/" + announced.getId() + "/upload-complete", Map.of())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("FILE_NOT_UPLOADED"));

    StoredFile uploaded = uploadInbound(link, CSV);
    LocalFileStore local = (LocalFileStore) store;
    local.markScanResult(uploaded.objectRef(), null);
    api.doPost("accountant", "/api/v1/files/" + uploaded.getId() + "/upload-complete", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scanStatus").value("PENDING"));
    api.doGet("accountant", "/api/v1/files/" + uploaded.getId() + "/link")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("FILE_SCAN_PENDING"));

    local.markScanResult(uploaded.objectRef(), "NO_THREATS_FOUND");
    scanJob.execute(today());
    assertThat(repository.findById(uploaded.getId()).orElseThrow().getScanStatus().name())
        .isEqualTo("CLEAN");
    api.doGet("accountant", "/api/v1/files/" + uploaded.getId() + "/link")
        .andExpect(status().isOk());
  }

  @Test
  void infectedFilesAreQuarantinedAndReported() throws Exception {
    JsonNode link = startInbound("ST-IN-2", CSV);
    StoredFile uploaded = uploadInbound(link, CSV);
    ((LocalFileStore) store).markScanResult(uploaded.objectRef(), "THREATS_FOUND");
    api.doPost("accountant", "/api/v1/files/" + uploaded.getId() + "/upload-complete", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scanStatus").value("QUARANTINED"));
    StoredFile quarantined = repository.findById(uploaded.getId()).orElseThrow();
    assertThat(quarantined.getObjectKey())
        .isEqualTo(ObjectKeys.QUARANTINE_PREFIX + uploaded.getObjectKey());
    assertThat(store.exists(uploaded.objectRef())).isFalse();
    assertThat(store.exists(quarantined.objectRef())).isTrue();
    assertThat(quarantined.getScanResult()).isEqualTo("THREATS_FOUND");

    api.doGet("accountant", "/api/v1/files/" + uploaded.getId() + "/link")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("FILE_QUARANTINED"));
    String id = String.valueOf(uploaded.getId());
    assertThat(
            jdbc.queryForList(
                "select recipient from msg_notification where entity_type = 'StoredFile'"
                    + " and entity_id = ?",
                String.class,
                id))
        .contains("accountant", "infosec");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from alt_alert where exception_code = 'FILE_QUARANTINED'"
                    + " and entity_id = ?",
                Integer.class,
                id))
        .isEqualTo(1);
    api.doGet("infosec", "/api/v1/files/quarantined")
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.content[*].id", org.hamcrest.Matchers.hasItem(uploaded.getId().intValue())));
    api.doGet("accountant", "/api/v1/files/quarantined").andExpect(status().isForbidden());
  }

  @Test
  void inboundUploadsNeedAnInboundClassAndAChecksum() throws Exception {
    api.doPost(
            "accountant",
            "/api/v1/files/inbound-uploads",
            Map.of(
                "ownerType",
                TYPE,
                "ownerId",
                "ST-IN-3",
                "recordClass",
                "GENERAL_DOCUMENT",
                "fileName",
                "a.csv",
                "sizeBytes",
                10,
                "sha256",
                Sha256.hex(CSV)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("RECORD_CLASS_NOT_INBOUND"));
    JsonNode link = startInbound("ST-IN-4", CSV);
    byte[] other = "other,content\n".getBytes(StandardCharsets.UTF_8);
    mvc.perform(
            as(
                "accountant",
                put(link.get("url").asText()).contentType(MediaType.TEXT_PLAIN).content(other)))
        .andExpect(status().isForbidden());
  }

  @Test
  void recordClassesAreParameterisedAndMappedToTheRetentionRules() throws Exception {
    api.doGet("holdofficer", "/api/v1/files/record-classes")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(11))
        .andExpect(jsonPath("$[?(@.code == 'STR')].legalHold").value(true))
        .andExpect(jsonPath("$[?(@.code == 'STR')].archiveToEcm").value(true));
    api.doGet("accountant", "/api/v1/files/record-classes").andExpect(status().isForbidden());
    Map<String, Object> mapped =
        Map.of(
            "retentionRecordType", "BROKER_CLAIM",
            "retentionPeriod", "P5Y",
            "legalHold", false,
            "archiveToEcm", false,
            "active", true);
    api.doPut("holdofficer", "/api/v1/files/record-classes/WORKING_FILE", mapped)
        .andExpect(status().isForbidden());
    api.doPut("holdapprover", "/api/v1/files/record-classes/WORKING_FILE", mapped)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.retentionRecordType").value("BROKER_CLAIM"));
    try {
      StoredFile claimMapped = storeAs("accountant", "ST-RET", "WORKING_FILE", PDF);
      assertThat(claimMapped.getRetentionUntil()).isEqualTo(today().plusYears(15));
    } finally {
      api.doPut(
              "holdapprover",
              "/api/v1/files/record-classes/WORKING_FILE",
              Map.of(
                  "retentionPeriod", "P5Y",
                  "legalHold", false,
                  "archiveToEcm", false,
                  "active", true))
          .andExpect(status().isOk());
    }
    StoredFile fallback = storeAs("accountant", "ST-RET", "WORKING_FILE", PDF);
    assertThat(fallback.getRetentionUntil()).isEqualTo(today().plusYears(5));
    StoredFile account = storeAs("accountant", "ST-RET", "POLICY_DOCUMENT", PDF);
    assertThat(account.getRetentionUntil()).isEqualTo(today().plusYears(20));
    api.doPut(
            "holdapprover",
            "/api/v1/files/record-classes/WORKING_FILE",
            Map.of("retentionPeriod", "20 years", "active", true))
        .andExpect(status().isBadRequest());
  }
}
