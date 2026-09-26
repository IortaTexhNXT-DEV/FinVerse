package com.iortatechnxt.brokerverse.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.common.storage.BucketClass;
import com.iortatechnxt.brokerverse.common.storage.FileStore;
import com.iortatechnxt.brokerverse.common.storage.ObjectRef;
import com.iortatechnxt.brokerverse.common.storage.StorageProperties;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.domain.HoldAction;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.domain.StoredFileRepository;
import com.iortatechnxt.brokerverse.storage.service.FileEcmArchiveJob;
import com.iortatechnxt.brokerverse.storage.service.FileOrphanReconciliationJob;
import com.iortatechnxt.brokerverse.storage.service.FileRetentionJob;
import com.iortatechnxt.brokerverse.storage.service.LegalHoldApprovalSource;
import com.iortatechnxt.brokerverse.storage.service.LegalHoldService;
import com.iortatechnxt.brokerverse.storage.service.StorageTopics;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.StorageTestOwnerAccess;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Document storage jobs and controls (ST0): legal hold as a controlled action, class holds,
 * retention delete (never under legal hold), removal of deleted files, abandoned uploads, orphan
 * clean-up and the ECM archive publisher.
 */
@IntegrationTest
class StorageJobsIT {

  private static final String TYPE = StorageTestOwnerAccess.TYPE;
  private static final byte[] PDF = "%PDF-1.4 jobs".getBytes(StandardCharsets.US_ASCII);

  @Autowired private Api api;
  @Autowired private AsUser as;
  @Autowired private StoredFileService files;
  @Autowired private StoredFileRepository repository;
  @Autowired private LegalHoldService holds;
  @Autowired private LegalHoldApprovalSource approvalSource;
  @Autowired private FileRetentionJob retentionJob;
  @Autowired private FileOrphanReconciliationJob orphanJob;
  @Autowired private FileEcmArchiveJob ecmJob;
  @Autowired private FileStore store;
  @Autowired private StorageProperties properties;
  @Autowired private JdbcTemplate jdbc;

  private StoredFile storeFile(String recordClass) {
    return as.run(
        "accountant",
        () ->
            files.store(
                new StoreRequest(
                    new FileOwner(null, TYPE, "SJ-" + UUID.randomUUID().toString().substring(0, 8)),
                    null,
                    recordClass,
                    "receipt.pdf",
                    PDF,
                    null)));
  }

  private StoredFile reload(StoredFile file) {
    return repository.findById(file.getId()).orElseThrow();
  }

  private static LocalDate today() {
    return LocalDate.now(ZoneOffset.UTC);
  }

  private void pastRetention(StoredFile file) {
    jdbc.update(
        "update stored_file set retention_until = current_date - 1 where id = ?", file.getId());
  }

  @Test
  void legalHoldIsAControlledActionWithApproverAndReason() throws Exception {
    StoredFile file = storeFile("GENERAL_DOCUMENT");
    String base = "/api/v1/files/" + file.getId() + "/legal-hold-requests";
    api.doPost("accountant", base, Map.of("action", "PLACE", "reason", "Audit"))
        .andExpect(status().isForbidden());
    JsonNode request =
        api.read(
            api.doPost("holdofficer", base, Map.of("action", "PLACE", "reason", "BIR audit 2026"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING")));
    long requestId = request.get("id").asLong();
    api.doPost("holdofficer", base, Map.of("action", "PLACE", "reason", "Twice"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("HOLD_REQUEST_PENDING"));
    assertThat(
            approvalSource.pendingFor(
                ApprovalViewer.user("holdapprover", Set.of("FILE_LEGAL_HOLD_APPROVE"))))
        .anyMatch(i -> i.reference().equals("LH-" + requestId));
    assertThat(
            approvalSource.pendingFor(
                ApprovalViewer.user("holdofficer", Set.of("FILE_LEGAL_HOLD_REQUEST"))))
        .isEmpty();
    api.doGet("holdapprover", "/api/v1/files/legal-hold-requests")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == " + requestId + ")].action").value("PLACE"));

    String approve = "/api/v1/files/legal-hold-requests/" + requestId + "/approve";
    api.doPost("holdofficer", approve, Map.of("reason", "ok")).andExpect(status().isForbidden());
    assertThatThrownBy(() -> as.run("holdofficer", () -> holds.decide(requestId, true, "own")))
        .extracting("code")
        .isEqualTo("HOLD_REQUEST_OWN");
    api.doPost("holdapprover", approve, Map.of("reason", "DOA approval"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"))
        .andExpect(jsonPath("$.decidedBy").value("holdapprover"));

    StoredFile held = reload(file);
    assertThat(held.isLegalHold()).isTrue();
    assertThat(held.getLegalHoldReason()).isEqualTo("BIR audit 2026");
    assertThat(held.getLegalHoldPlacedBy()).isEqualTo("holdofficer");
    assertThat(held.getLegalHoldApprovedBy()).isEqualTo("holdapprover");
    assertThat(store.metadata(held.objectRef()).orElseThrow().legalHold()).isTrue();

    assertThatThrownBy(
            () ->
                as.run(
                    "accountant",
                    () -> {
                      files.delete(file.getId());
                      return null;
                    }))
        .extracting("code")
        .isEqualTo("FILE_UNDER_LEGAL_HOLD");
    pastRetention(file);
    JobOutcome kept = retentionJob.execute(today());
    assertThat(kept.message()).contains("kept under legal hold");
    assertThat(reload(file).getPurgedAt()).isNull();
    assertThat(store.exists(file.objectRef())).isTrue();

    long release =
        as.run("holdofficer", () -> holds.request(file.getId(), HoldAction.RELEASE, "Audit closed"))
            .getId();
    api.doPost(
            "holdapprover",
            "/api/v1/files/legal-hold-requests/" + release + "/reject",
            Map.of("reason", "Keep until the BIR letter"))
        .andExpect(jsonPath("$.status").value("REJECTED"));
    assertThat(reload(file).isLegalHold()).isTrue();
    long release2 =
        as.run(
                "holdofficer",
                () -> holds.request(file.getId(), HoldAction.RELEASE, "Letter received"))
            .getId();
    as.run("holdapprover", () -> holds.decide(release2, true, "Released"));
    assertThat(reload(file).isLegalHold()).isFalse();
    api.doGet("auditor", base)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));

    retentionJob.execute(today());
    assertThat(reload(file).getPurgedAt()).isNotNull();
    assertThat(store.exists(file.objectRef())).isFalse();
    assertThatThrownBy(
            () ->
                as.run("holdofficer", () -> holds.request(file.getId(), HoldAction.PLACE, "late")))
        .hasMessageContaining(String.valueOf(file.getId()));
  }

  @Test
  void recordClassesUnderHoldAreHeldFromTheStart() {
    StoredFile receipt = storeFile("OFFICIAL_RECEIPT");
    assertThat(receipt.isLegalHold()).isTrue();
    assertThat(receipt.getLegalHoldPlacedBy()).isEqualTo("SYSTEM");
    assertThat(receipt.getLegalHoldReason()).contains("OFFICIAL_RECEIPT");
    assertThat(store.metadata(receipt.objectRef()).orElseThrow().legalHold()).isTrue();
    assertThatThrownBy(
            () ->
                as.run(
                    "holdofficer", () -> holds.request(receipt.getId(), HoldAction.PLACE, "again")))
        .extracting("code")
        .isEqualTo("FILE_ALREADY_HELD");
    StoredFile general = storeFile("GENERAL_DOCUMENT");
    assertThatThrownBy(
            () ->
                as.run(
                    "holdofficer", () -> holds.request(general.getId(), HoldAction.RELEASE, "no")))
        .extracting("code")
        .isEqualTo("FILE_NOT_HELD");
    assertThatThrownBy(
            () ->
                as.run("holdofficer", () -> holds.request(general.getId(), HoldAction.PLACE, " ")))
        .extracting("code")
        .isEqualTo("REASON_REQUIRED");
  }

  @Test
  void retentionRemovesExpiredDeletedAndAbandonedFiles() throws Exception {
    StoredFile expired = storeFile("GENERAL_DOCUMENT");
    pastRetention(expired);
    StoredFile deleted = storeFile("GENERAL_DOCUMENT");
    as.run(
        "accountant",
        () -> {
          files.delete(deleted.getId());
          return null;
        });
    StoredFile recentlyDeleted = storeFile("GENERAL_DOCUMENT");
    as.run(
        "accountant",
        () -> {
          files.delete(recentlyDeleted.getId());
          return null;
        });
    jdbc.update(
        "update stored_file set deleted_at = now() - interval '31 days' where id = ?",
        deleted.getId());
    StoredFile live = storeFile("GENERAL_DOCUMENT");
    JsonNode started =
        api.read(
            api.doPost(
                    "accountant",
                    "/api/v1/files/inbound-uploads",
                    Map.of(
                        "ownerType",
                        TYPE,
                        "ownerId",
                        "SJ-ABANDONED",
                        "recordClass",
                        "INBOUND_FILE",
                        "fileName",
                        "never.csv",
                        "sizeBytes",
                        10,
                        "sha256",
                        Sha256.hex("never")))
                .andExpect(status().isCreated()));
    long abandoned = started.get("fileId").asLong();
    jdbc.update(
        "update stored_file set created_at = now() - interval '25 hours' where id = ?", abandoned);

    retentionJob.execute(today());

    assertThat(reload(expired).getPurgedAt()).isNotNull();
    assertThat(reload(expired).getDeletedBy()).isEqualTo("SYSTEM");
    assertThat(store.exists(expired.objectRef())).isFalse();
    assertThat(reload(deleted).getPurgedAt()).isNotNull();
    assertThat(store.exists(deleted.objectRef())).isFalse();
    assertThat(reload(recentlyDeleted).getPurgedAt()).isNull();
    assertThat(store.exists(recentlyDeleted.objectRef())).isTrue();
    assertThat(reload(live).getPurgedAt()).isNull();
    assertThat(repository.findById(abandoned).orElseThrow().getDeletedAt()).isNotNull();
    Integer audited =
        jdbc.queryForObject(
            "select count(*) from audit_log where entity_type = 'StoredFile' and entity_id = ?"
                + " and summary like 'Object removed (retention ended)%'",
            Integer.class, String.valueOf(expired.getId()));
    assertThat(audited).isEqualTo(1);
  }

  @Test
  void orphanObjectsAreDeletedAfterADay() throws Exception {
    ObjectRef orphan =
        new ObjectRef(BucketClass.DOCUMENTS, "shared/orphan/2026/09/" + UUID.randomUUID());
    store.put(orphan, PDF, "application/pdf", Sha256.hex(PDF));
    ObjectRef freshOrphan =
        new ObjectRef(BucketClass.DOCUMENTS, "shared/orphan/2026/09/" + UUID.randomUUID());
    store.put(freshOrphan, PDF, "application/pdf", Sha256.hex(PDF));
    ObjectRef heldOrphan =
        new ObjectRef(BucketClass.DOCUMENTS, "shared/orphan/2026/09/" + UUID.randomUUID());
    store.put(heldOrphan, PDF, "application/pdf", Sha256.hex(PDF));
    store.legalHold(heldOrphan, true);
    StoredFile recorded = storeFile("GENERAL_DOCUMENT");
    FileTime old = FileTime.from(Instant.now().minus(2, ChronoUnit.DAYS));
    for (ObjectRef ref : new ObjectRef[] {orphan, heldOrphan, recorded.objectRef()}) {
      Files.setLastModifiedTime(objectPath(ref), old);
    }

    JobOutcome outcome = orphanJob.execute(today());

    assertThat(outcome.itemsProcessed()).isGreaterThanOrEqualTo(1);
    assertThat(store.exists(orphan)).isFalse();
    assertThat(store.exists(freshOrphan)).isTrue();
    assertThat(store.exists(heldOrphan)).isTrue();
    assertThat(store.exists(recorded.objectRef())).isTrue();
    store.legalHold(heldOrphan, false);
    store.delete(heldOrphan);
    store.delete(freshOrphan);
  }

  private Path objectPath(ObjectRef ref) {
    return properties
        .local()
        .root()
        .toAbsolutePath()
        .normalize()
        .resolve(ref.bucket().name().toLowerCase(java.util.Locale.ROOT))
        .resolve("objects")
        .resolve(ref.key());
  }

  @Test
  void finalRecordsOfEcmClassesArePublishedThroughTheOutbox() {
    StoredFile policy = storeFile("POLICY_DOCUMENT");
    assertThat(policy.getEcmStatus().name()).isEqualTo("AWAITING_FINAL");
    ecmJob.execute(today());
    assertThat(reload(policy).getEcmStatus().name()).isEqualTo("AWAITING_FINAL");

    as.run("accountant", () -> files.markFinal(policy.getId()));
    assertThat(reload(policy).getEcmStatus().name()).isEqualTo("PENDING");
    assertThat(ecmJob.execute(today()).itemsProcessed()).isGreaterThanOrEqualTo(1);
    StoredFile requested = reload(policy);
    assertThat(requested.getEcmStatus().name()).isEqualTo("REQUESTED");
    assertThat(requested.getFinalAt()).isNotNull();
    Map<String, Object> row =
        jdbc.queryForMap(
            "select topic, event_type, payload::text as payload from evt_outbox where event_key = ?",
            "stored-file:" + policy.getId());
    assertThat(row.get("topic")).isEqualTo(StorageTopics.ECM_ARCHIVE_REQUESTED);
    assertThat(row.get("event_type")).isEqualTo(StorageTopics.TYPE_ECM_ARCHIVE_REQUESTED);
    assertThat(row.get("payload").toString())
        .contains("POLICY_DOCUMENT")
        .contains(policy.getSha256())
        .doesNotContain("receipt.pdf");

    StoredFile archived = files.recordEcmReference(policy.getId(), " ECM-2026-000123 ");
    assertThat(archived.getEcmStatus().name()).isEqualTo("ARCHIVED");
    assertThat(archived.getEcmReference()).isEqualTo("ECM-2026-000123");

    StoredFile general = storeFile("GENERAL_DOCUMENT");
    as.run("accountant", () -> files.markFinal(general.getId()));
    assertThat(reload(general).getEcmStatus().name()).isEqualTo("NOT_REQUIRED");
    assertThatThrownBy(() -> files.recordEcmReference(general.getId(), "ECM-1"))
        .extracting("code")
        .isEqualTo("FILE_NOT_FOR_ECM");
    assertThatThrownBy(() -> files.recordEcmReference(general.getId(), " "))
        .extracting("code")
        .isEqualTo("ECM_REFERENCE_REQUIRED");
  }
}
