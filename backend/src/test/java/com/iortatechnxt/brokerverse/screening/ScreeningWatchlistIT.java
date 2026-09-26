package com.iortatechnxt.brokerverse.screening;

import static com.iortatechnxt.brokerverse.screening.ScreeningSetupFixtures.csv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.AliasType;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.ChangeStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.ChangeType;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryValues;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionError;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionRun;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionTrigger;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.RunStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistChange;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistEntry;
import com.iortatechnxt.brokerverse.screening.watchlist.service.IngestErrorDigest;
import com.iortatechnxt.brokerverse.screening.watchlist.service.ListFileService;
import com.iortatechnxt.brokerverse.screening.watchlist.service.ListedEntry;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistApprovalSource;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistBulkHandler;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistDecisionService;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistDirectory;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistIngestJob;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Watchlists (SNSRP-201-204; FR-SS-020 to 023): manual maintenance under maker-checker, list file
 * upload (run log, failed records, changes PENDING until approved, then ACTIVE), scheduled runs of
 * the official feed with delisting, the failed-records digest, the approval inbox and the bulk
 * handler.
 */
@IntegrationTest
class ScreeningWatchlistIT {

  private static final String MAKER = "compoff";
  private static final String CHECKER = "compchk";
  private static final String PEP = "PEP";

  @Autowired private WatchlistService watchlists;
  @Autowired private ListFileService listFiles;
  @Autowired private WatchlistDecisionService decisions;
  @Autowired private WatchlistDirectory directory;
  @Autowired private WatchlistApprovalSource approvals;
  @Autowired private WatchlistBulkHandler handler;
  @Autowired private WatchlistIngestJob job;
  @Autowired private IngestErrorDigest digest;
  @Autowired private ScreeningSetupFixtures fx;
  @Autowired private AsUser asUser;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Clock clock;
  @Autowired private SystemParameterService parameters;

  private <T> T as(String user, Supplier<T> action) {
    return asUser.run(user, action);
  }

  private static EntryValues person(String name) {
    return new EntryValues(
        "INTERNAL",
        SubjectType.INDIVIDUAL,
        name,
        null,
        null,
        LocalDate.of(1970, 2, 28),
        "Filipino",
        null,
        null,
        null,
        List.of(new EntryValues.Alias(name + " Alias", AliasType.AKA)));
  }

  private boolean screened(Long entryId) {
    return directory.active(List.of()).stream().anyMatch(e -> e.id().equals(entryId));
  }

  @Test
  void aManualEntryIsPendingUntilTheCheckerApprovesIt() {
    String name = "Pedro Invented Lagman " + fx.unique();
    WatchlistChange add =
        as(MAKER, () -> watchlists.add(null, person(name), "Internal finding UAT"));
    WatchlistEntry entry = watchlists.entry(add.getEntryId());
    assertThat(entry.getStatus()).isEqualTo(EntryStatus.PENDING);
    assertThat(entry.getExternalRef()).startsWith("INT-");
    assertThat(screened(entry.getId())).isFalse();
    assertThat(
            approvals.pendingFor(ApprovalViewer.user(CHECKER, Set.of("SCR_LIST_APPROVE"))).stream()
                .map(PendingApproval::reference))
        .contains(entry.getExternalRef());
    assertThat(approvals.pendingFor(ApprovalViewer.user(MAKER, Set.of("SCR_LIST_APPROVE"))))
        .extracting(PendingApproval::reference)
        .doesNotContain(entry.getExternalRef());
    assertThatThrownBy(
            () -> as(MAKER, () -> watchlists.change(entry.getId(), person(name), "again")))
        .hasMessage(
            "Entry " + entry.getExternalRef() + " already has a change waiting for approval");

    as(CHECKER, () -> decisions.approve(add.getId(), null));
    WatchlistEntry active = watchlists.entry(entry.getId());
    assertThat(active.getStatus()).isEqualTo(EntryStatus.ACTIVE);
    assertThat(active.getEffectiveFrom()).isEqualTo(LocalDate.now(clock));
    assertThat(active.getEntryVersion()).isEqualTo(1);
    ListedEntry listed = directory.entry(entry.getId()).orElseThrow();
    assertThat(listed.aliases()).containsExactly(name + " Alias");
    assertThat(listed.sourceCode()).isEqualTo("INTERNAL");
    assertThat(screened(entry.getId())).isTrue();
    assertThatThrownBy(() -> as(CHECKER, () -> decisions.approve(add.getId(), null)))
        .hasMessageContaining("already approved");

    WatchlistChange alias =
        as(MAKER, () -> watchlists.change(entry.getId(), person(name + " Jr"), "New alias"));
    assertThat(watchlists.readValues(alias.getBeforeValues()).primaryName()).isEqualTo(name);
    assertThatThrownBy(() -> as(CHECKER, () -> decisions.reject(alias.getId(), "")))
        .hasMessage("Enter the remarks for the rejection");
    as(CHECKER, () -> decisions.reject(alias.getId(), "Alias not in the advisory"));
    assertThat(watchlists.entry(entry.getId()).getPrimaryName()).isEqualTo(name);
    assertThat(watchlists.change(alias.getId()).getStatus()).isEqualTo(ChangeStatus.REJECTED);

    WatchlistChange off = as(MAKER, () -> watchlists.deactivate(entry.getId(), "Delisted by AMLC"));
    assertThat(off.getChangeType()).isEqualTo(ChangeType.DEACTIVATE);
    as(CHECKER, () -> decisions.approve(off.getId(), "ok"));
    WatchlistEntry inactive = watchlists.entry(entry.getId());
    assertThat(inactive.getStatus()).isEqualTo(EntryStatus.INACTIVE);
    assertThat(inactive.getDelistedOn()).isEqualTo(LocalDate.now(clock));
    assertThat(watchlists.history(entry.getId())).hasSize(3);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_log where entity_type = 'WatchlistEntry' and entity_id = ?",
                Integer.class,
                entry.getExternalRef()))
        .isGreaterThanOrEqualTo(4);
  }

  @Test
  void manualChangesAreValidatedAndTheMakerNeverApproves() {
    assertThatThrownBy(() -> as(MAKER, () -> watchlists.add(null, person(" "), "remarks")))
        .hasMessage("Enter the name of the listed person or entity");
    assertThatThrownBy(() -> as(MAKER, () -> watchlists.add(null, person("Some Invented"), " ")))
        .hasMessage("Enter the reason for the change");
    EntryValues noType =
        new EntryValues(
            null,
            SubjectType.ENTITY,
            "Invented Trading",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    assertThatThrownBy(() -> as(MAKER, () -> watchlists.add(null, noType, "remarks")))
        .hasMessage("Select the list type");

    WatchlistChange own =
        as("compdual", () -> watchlists.add(null, person("Dual Invented " + fx.unique()), "dual"));
    assertThatThrownBy(() -> as("compdual", () -> decisions.approve(own.getId(), null)))
        .hasMessage("A list change is approved by someone other than its maker");
    as(CHECKER, () -> decisions.reject(own.getId(), "Not needed"));
    assertThat(watchlists.entry(own.getEntryId()).getStatus()).isEqualTo(EntryStatus.DRAFT);
    WatchlistChange again =
        as(MAKER, () -> watchlists.change(own.getEntryId(), person("Dual Invented Two"), "retry"));
    assertThat(again.getChangeType()).isEqualTo(ChangeType.ADD);
    assertThat(watchlists.entry(own.getEntryId()).getStatus()).isEqualTo(EntryStatus.PENDING);
  }

  @Test
  void anUploadedFileIsLoggedAndItsEntriesArePendingUntilApproved() {
    String u = fx.unique();
    byte[] file =
        csv(
            u
                + "-1,INDIVIDUAL,Maria Invented Santos "
                + u
                + ",Maria,Santos,MIS;M. Santos,1971-03-04,Filipino,,2026-01-10,,Advisory",
            u + "-2,INDIVIDUAL,Jose Invented Reyes " + u + ",Jose,Reyes,,,,,,,",
            u + "-3,ENTITY,Invented Holdings " + u + " Inc,,,,,,,,,",
            u + "-4,INDIVIDUAL,,,,,,,,,,",
            u + "-5,INDIVIDUAL,Bad Date Invented " + u + ",,,,31-02-1970,,,,,");
    IngestionRun run = as(MAKER, () -> listFiles.upload("NLDS_PEP", "nlds_" + u + ".csv", file));
    assertThat(run.getTrigger()).isEqualTo(IngestionTrigger.MANUAL_UPLOAD);
    assertThat(run.getStatus()).isEqualTo(RunStatus.PARTIAL);
    assertThat(run.getReceived()).isEqualTo(5);
    assertThat(run.getAdded()).isEqualTo(3);
    assertThat(run.getFailed()).isEqualTo(2);
    assertThat(run.isPendingApproval()).isTrue();
    assertThat(listFiles.errors(run.getId()))
        .extracting(IngestionError::getReason)
        .containsExactly("Line 5: name is missing", "Line 6: birth date is not a valid date");
    assertThat(watchlists.pendingOfRun(run.getId())).isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from alt_alert where dedup_key = ?",
                Integer.class,
                "SCR_INGEST_FAILED:" + run.getRunNo()))
        .isEqualTo(1);
    List<Long> ids =
        jdbc.queryForList(
            "select entry_id from scr_watchlist_change where run_id = ?", Long.class, run.getId());
    assertThat(ids).allMatch(id -> watchlists.entry(id).getStatus() == EntryStatus.PENDING);

    IngestionRun again = as(MAKER, () -> listFiles.upload("NLDS_PEP", "again.csv", file));
    assertThat(again.getAdded()).isZero();
    assertThat(again.getUpdated()).isZero();
    assertThat(again.getUnchanged()).isEqualTo(3);

    assertThat(as(CHECKER, () -> decisions.approveRun(run.getId()))).isEqualTo(3);
    assertThat(ids).allMatch(id -> watchlists.entry(id).getStatus() == EntryStatus.ACTIVE);
    assertThat(directory.entries(ids))
        .filteredOn(e -> e.primaryName().startsWith("Maria"))
        .singleElement()
        .satisfies(
            e -> {
              assertThat(e.listType()).isEqualTo(PEP);
              assertThat(e.aliases()).containsExactly("MIS", "M. Santos");
              assertThat(e.birthDate()).isEqualTo(LocalDate.of(1971, 3, 4));
            });

    assertThatThrownBy(
            () ->
                as(
                    MAKER,
                    () ->
                        listFiles.upload(
                            "AML_ADVISORY",
                            "advisory.pdf",
                            "%PDF-1.4".getBytes(StandardCharsets.UTF_8))))
        .hasMessage("The file type is not allowed for source AML_ADVISORY");
  }

  @Test
  void theScheduledFeedAppliesTheOfficialListAndDelistsMissingEntries() {
    String source = fx.source("SANCTION", true);
    String u = fx.unique();
    as(
        MAKER,
        () ->
            listFiles.stage(
                source,
                "list1.csv",
                csv(
                    u + "-1,INDIVIDUAL,Juan Invented Cruz " + u + ",,,,,,,,,",
                    u + "-2,INDIVIDUAL,Ana Invented Luna " + u + ",,,,,,,,,",
                    u + "-3,ENTITY,Invented Shipping " + u + ",,,,,,,,,")));
    JobOutcome outcome = job.execute(LocalDate.now(clock));
    assertThat(outcome.message()).contains("list run(s)");
    IngestionRun first = latestRun(source);
    assertThat(first.getTrigger()).isEqualTo(IngestionTrigger.SCHEDULED);
    assertThat(first.getStatus()).isEqualTo(RunStatus.SUCCESS);
    assertThat(first.getAdded()).isEqualTo(3);
    List<ListedEntry> listed =
        directory.active(List.of("SANCTION")).stream()
            .filter(e -> source.equals(e.sourceCode()))
            .toList();
    assertThat(listed).hasSize(3);

    as(
        MAKER,
        () ->
            listFiles.stage(
                source,
                "list2.csv",
                csv(
                    u + "-1,INDIVIDUAL,Juan Invented Cruz " + u + ",,,,,,,,,",
                    u + "-2,INDIVIDUAL,Ana Invented Luna Changed " + u + ",,,,,,,,,")));
    job.execute(LocalDate.now(clock));
    IngestionRun second = latestRun(source);
    assertThat(second.getUpdated()).isEqualTo(1);
    assertThat(second.getDelisted()).isEqualTo(1);
    assertThat(second.getUnchanged()).isEqualTo(1);
    Map<String, Object> delisted =
        jdbc.queryForMap(
            "select e.status, e.delisted_on from scr_watchlist_entry e"
                + " join scr_watchlist_source s on s.id = e.source_id"
                + " where s.code = ? and e.external_ref = ?",
            source,
            u + "-3");
    assertThat(delisted.get("status")).isEqualTo("INACTIVE");
    assertThat(delisted.get("delisted_on")).isNotNull();

    job.execute(LocalDate.now(clock));
    IngestionRun none = latestRun(source);
    assertThat(none.getStatus()).isEqualTo(RunStatus.FAILED);
    assertThat(none.getError()).isEqualTo("No list file was received for source " + source);

    as(
        MAKER,
        () ->
            listFiles.stage(
                source, "broken.csv", "no,header\n1,2\n".getBytes(StandardCharsets.UTF_8)));
    job.execute(LocalDate.now(clock));
    IngestionRun broken = latestRun(source);
    assertThat(broken.getStatus()).isEqualTo(RunStatus.FAILED);
    assertThat(broken.getError()).contains("missing column(s)");
  }

  private IngestionRun latestRun(String source) {
    return listFiles
        .runs(source, org.springframework.data.domain.PageRequest.of(0, 1))
        .getContent()
        .get(0);
  }

  @Test
  void theDigestSendsTheFailedRecordsToTheRecipients() {
    String u = fx.unique();
    as(
        MAKER,
        () ->
            listFiles.upload(
                "NLDS_PEP",
                "digest.csv",
                csv(u + "-1,INDIVIDUAL,,,,,,,,,,", u + "-2,ALIEN,X,,,,,,,,,")));
    String key = "SCR_INGEST_ALERT_RECIPIENTS";
    String before = parameters.text(key, "");
    try {
      as("admin", () -> parameters.update(key, ""));
      assertThat(digest.send().message()).contains("no recipients");
      as("admin", () -> parameters.update(key, "compliance@bdoi-test.ph, compliance@@test"));
      JobOutcome sent = digest.send();
      assertThat(sent.itemsProcessed()).isGreaterThanOrEqualTo(2);
      assertThat(sent.message()).endsWith("sent to 1");
      assertThat(digest.send().message())
          .isEqualTo("No failed watchlist record since the last digest");
    } finally {
      as("admin", () -> parameters.update(key, before));
    }
  }

  @Test
  void theBulkHandlerChecksAndSubmitsListRecords() {
    String u = fx.unique();
    BulkContext context = new BulkContext(1L, "BLK-T-" + u, LocalDate.now(clock), Map.of());
    assertThat(handler.columns()).hasSize(12);
    assertThat(handler.validate(new BulkRow(2, Map.of("Reference", u)), context))
        .containsExactly("Line 2: name is missing");
    String ref =
        handler.commit(
            new BulkRow(
                3,
                Map.of(
                    "Reference", u, "Entity Type", "ENTITY", "Primary Name", "Invented Corp " + u)),
            context);
    assertThat(ref).isEqualTo(u);
    Long entryId =
        jdbc.queryForObject(
            "select id from scr_watchlist_entry where external_ref = ?", Long.class, u);
    assertThat(watchlists.entry(entryId).getStatus()).isEqualTo(EntryStatus.PENDING);
    assertThat(watchlists.entry(entryId).getListType()).isEqualTo("INTERNAL");
  }
}
