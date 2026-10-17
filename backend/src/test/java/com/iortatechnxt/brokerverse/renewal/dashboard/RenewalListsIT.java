package com.iortatechnxt.brokerverse.renewal.dashboard;

import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.AO;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.PO;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.PROC_TL;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.TL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.renewal.RenewalFixtures;
import com.iortatechnxt.brokerverse.renewal.audit.service.RenewalAuditLogService;
import com.iortatechnxt.brokerverse.renewal.candidate.api.CandidateRowMapper;
import com.iortatechnxt.brokerverse.renewal.candidate.service.AccountHistoryService;
import com.iortatechnxt.brokerverse.renewal.candidate.service.BucketPanels;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Codes;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Flags;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Tab;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateQueryService;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalAssignmentService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalDispositionService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalDispositionService.Input;
import com.iortatechnxt.brokerverse.renewal.service.RenewalScope;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

/**
 * Renewal lists of BDOI's FRS against the real database (FRRN.001.02, FRRN.002.05, FRRN.002.08,
 * FRRN.003.05, FRRN.004.06, FRRN.043): the bucket panels, BDOI's list columns, the sort of a list,
 * the data scope of a Processing Officer, the RMEL file name and the audit logs.
 */
@IntegrationTest
class RenewalListsIT {

  @Autowired private RenewalFixtures fx;
  @Autowired private CandidateQueryService queries;
  @Autowired private CandidateRowMapper rows;
  @Autowired private BucketPanels panels;
  @Autowired private RenewalScope scope;
  @Autowired private RenewalAssignmentService assignments;
  @Autowired private AccountHistoryService history;
  @Autowired private RenewalDispositionService dispositions;
  @Autowired private RenewalAuditLogService audit;
  @Autowired private SystemParameterService parameters;
  @Autowired private AsUser as;

  private CandidateFilter tab(Tab tab, String search) {
    return new CandidateFilter(fx.company(), tab, search, null, null, Codes.NONE, Flags.NONE);
  }

  private List<String> refs(String user, Tab tab, String search) {
    return as.run(user, () -> queries.list(panels.apply(tab(tab, search)), 0, 100)).stream()
        .map(RenewalCandidate::getRenewalRef)
        .toList();
  }

  @Test
  void eachRenewalShowsInThePanelOfItsBucketAndNotForRenewalInNonRenewable() {
    RenewalCandidate c = fx.unassignedRetail();
    String ref = c.getRenewalRef();
    Tab panel = c.getBucket() == Bucket.CLEAN ? Tab.BUCKET_CLEAN : Tab.BUCKET_REVIEW;
    assertThat(refs(TL, panel, ref)).containsExactly(ref);
    assertThat(refs(TL, Tab.BUCKET_NON_RENEWABLE, ref)).isEmpty();
    assertThat(refs(TL, Tab.ALL, ref)).containsExactly(ref);

    as.run(TL, () -> assignments.assign(fx.company(), List.of(ref), AO, null));
    as.run(AO, () -> history.open(fx.company(), ref));
    as.run(
        AO,
        () ->
            dispositions.save(
                fx.company(),
                ref,
                new Input(RenewalDisposition.NOT_FOR_RENEWAL, "UNIT_SOLD", null, null, "Sold")));
    assertThat(refs(TL, Tab.BUCKET_NON_RENEWABLE, ref)).containsExactly(ref);
    assertThat(refs(TL, Tab.BUCKET_CLEAN, ref)).isEmpty();
    assertThat(refs(TL, Tab.BUCKET_REVIEW, ref)).isEmpty();
  }

  @Test
  void rowsCarryBdoisColumnsAndTheListSortsBothWays() {
    RenewalCandidate c = fx.unassignedRetail();
    var row = as.run(TL, () -> rows.row(fx.reload(c), queries.scope(fx.company())));
    assertThat(row.bdoi().statusName()).isEqualTo("In Process Renewal");
    assertThat(row.bdoi().commissionAmount()).isNotNull();
    var asc =
        as.run(TL, () -> queries.list(tab(Tab.ALL, null), 0, 50, BucketPanels.sort("expiry,asc")));
    var desc =
        as.run(TL, () -> queries.list(tab(Tab.ALL, null), 0, 50, BucketPanels.sort("expiry,desc")));
    assertThat(asc.getContent().get(0).getExpiryDate())
        .isBeforeOrEqualTo(desc.getContent().get(0).getExpiryDate());
    assertThat(BucketPanels.sort("unknown,asc")).isNull();
    assertThat(BucketPanels.sort("expiry,desc").getOrderFor("snapshot.expiryDate").getDirection())
        .isEqualTo(Sort.Direction.DESC);
  }

  @Test
  void aProcessingOfficerSeesOnlyTheRenewalsAssignedToHimWhenTheScopeIsAssigned() {
    RenewalCandidate c = fx.unassignedRetail();
    assertThat(as.run(PO, () -> scope.current(fx.company())).kind())
        .isEqualTo(RenewalScope.Kind.ALL);
    assertThat(refs(PO, Tab.ALL, c.getRenewalRef())).containsExactly(c.getRenewalRef());
    as.run("badmin", () -> parameters.update(RenewalScope.PROCESSING_SCOPE, "ASSIGNED"));
    try {
      assertThat(as.run(PO, () -> scope.current(fx.company())).kind())
          .isEqualTo(RenewalScope.Kind.ASSIGNED_PO);
      assertThat(as.run(PROC_TL, () -> scope.current(fx.company())).kind())
          .isEqualTo(RenewalScope.Kind.ALL);
      assertThat(refs(PO, Tab.ALL, c.getRenewalRef())).isEmpty();
      assertThatThrownBy(() -> as.run(PO, () -> queries.get(fx.company(), c.getRenewalRef())))
          .isInstanceOf(ResourceNotFoundException.class);
    } finally {
      as.run("badmin", () -> parameters.update(RenewalScope.PROCESSING_SCOPE, "ALL"));
    }
  }

  @Test
  void theRmelDownloadIsNamedByTheListMonthAndTheExtractionDate() {
    assertThat(BucketPanels.rmelFileName(LocalDate.of(2027, 3, 1), LocalDate.of(2026, 10, 10)))
        .isEqualTo("RMEL_MAR_2027_10102026.xlsx");
    assertThat(BucketPanels.rmelFileName(null, LocalDate.of(2026, 10, 10)))
        .isEqualTo("RMEL_OCT_2026_10102026.xlsx");
  }

  @Test
  void theAuditLogListsTheStatusChangesWithOldAndNewValuesAndExports() {
    RenewalCandidate c = fx.unassignedRetail();
    String ref = c.getRenewalRef();
    as.run(TL, () -> assignments.assign(fx.company(), List.of(ref), AO, null));
    var entries =
        as.run(
            TL,
            () ->
                audit.entries(
                    fx.company(), new RenewalAuditLogService.Query(null, null, null, ref, null)));
    assertThat(entries).isNotEmpty();
    // every entry shows when it happened (date and time, not only the date)
    assertThat(entries).allMatch(e -> e.at() != null);
    assertThat(entries)
        .anyMatch(
            e ->
                "Update Status".equals(e.actionType())
                    && e.oldValue() != null
                    && e.newValue() != null
                    && TL.equals(e.performedBy()));
    var file =
        as.run(
            TL,
            () ->
                audit.export(
                    fx.company(),
                    new RenewalAuditLogService.Query(null, null, "Update Status", ref, null),
                    true));
    assertThat(file.fileName()).matches("Audit Logs_\\d{8}\\.csv");
    assertThat(new String(file.content(), StandardCharsets.UTF_8))
        .startsWith("Timestamp,Module,Reference Number")
        .contains(ref);
  }
}
