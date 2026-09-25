package com.iortatechnxt.brokerverse.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.cashiering.service.PickupService;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.AssignmentKind;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.ItemStatus;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.TaggingOwner;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.service.ChangeRecorder;
import com.iortatechnxt.brokerverse.collections.common.service.CollectionItems;
import com.iortatechnxt.brokerverse.collections.disposition.domain.PrDisposition;
import com.iortatechnxt.brokerverse.collections.disposition.service.AccountTimelineService;
import com.iortatechnxt.brokerverse.collections.disposition.service.EffortService;
import com.iortatechnxt.brokerverse.collections.disposition.service.EffortService.EffortCommand;
import com.iortatechnxt.brokerverse.collections.disposition.service.PrDispositionService;
import com.iortatechnxt.brokerverse.collections.disposition.service.PrDispositionService.DispositionCommand;
import com.iortatechnxt.brokerverse.collections.feed.service.OutboxService;
import com.iortatechnxt.brokerverse.collections.worklist.domain.AssignmentRule.Details;
import com.iortatechnxt.brokerverse.collections.worklist.domain.RuleCriteria;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentDefaults;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentRuleService;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentService;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentService.ItemSelection;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentService.Reassign;
import com.iortatechnxt.brokerverse.collections.worklist.service.EditLockService;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter.Scope;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistQueryService;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistQueryService.GroupBy;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistRefreshJob;
import com.iortatechnxt.brokerverse.commission.domain.DpItemRepository;
import com.iortatechnxt.brokerverse.commission.service.DpListService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.service.port.CollectionFeed;
import com.iortatechnxt.brokerverse.opsledger.service.port.FeedItem;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.domain.JobTrigger;
import com.iortatechnxt.brokerverse.system.service.JobRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The Collections core against the real ledger, Cashiering and Commission: the worklist refresh and
 * its rules (BRCLXN.001-015), assignment (052), dispositions with their hand-offs through the
 * in-app CollectionFeed (016-023, COLLECTIONS_DESIGN 2.2), the inbox, efforts, the edit lock and
 * the field-level change log (043).
 */
@IntegrationTest
class CollectionsWorklistIT {

  @Autowired private CollectionsFixtures fx;
  @Autowired private CollectionItems items;
  @Autowired private PrDispositionService dispositions;
  @Autowired private EffortService efforts;
  @Autowired private AssignmentService assignments;
  @Autowired private AssignmentDefaults defaults;
  @Autowired private AssignmentRuleService rules;
  @Autowired private EditLockService locks;
  @Autowired private WorklistQueryService worklist;
  @Autowired private AccountTimelineService timeline;
  @Autowired private ChangeRecorder changes;
  @Autowired private CollectionFeed feed;
  @Autowired private OutboxService outbox;
  @Autowired private PickupService pickups;
  @Autowired private DpListService dpLists;
  @Autowired private DpItemRepository dpItems;
  @Autowired private FeedReadyEvents ready;
  @Autowired private JobRegistry jobs;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;

  @AfterEach
  void drainOutbox() {
    fx.drainOutbox();
  }

  private List<PrDisposition> dispose(
      String user, String invoiceNo, String code, Map<String, String> details) {
    return as.run(
        user,
        () ->
            dispositions.record(
                fx.company(),
                new DispositionCommand(List.of(invoiceNo), code, "Test " + code, null, details)));
  }

  @Test
  void aBookedInvoiceIsListedAssignedByRuleAndCompletedWhenPaid() {
    CollectionItem item = fx.listedMotor();
    String no = item.getInvoiceNo();

    assertThat(item.getStatus()).isEqualTo(ItemStatus.OPEN);
    assertThat(item.getCurrentHandler()).isEqualTo(CollectionsFixtures.HANDLER);
    assertThat(item.getClassification().segment()).isEqualTo("CBG");
    assertThat(item.getFigures().netOutstanding()).isPositive();
    assertThat(item.getFigures().agingBracket()).isNotNull();
    assertThat(item.getBalances()).isNotEmpty();
    assertThat(assignments.history(item.getId()))
        .first()
        .satisfies(a -> assertThat(a.getKind()).isEqualTo(AssignmentKind.RULE));

    fx.pay(no, "APP:" + BookingFixtures.token(), new BigDecimal("0.5"));
    CollectionItem partly = items.require(no);
    assertThat(partly.getStatus()).isEqualTo(ItemStatus.OPEN);
    assertThat(partly.getFigures().netOutstanding()).isLessThan(item.getFigures().netOutstanding());

    fx.pay(no, "APP:" + BookingFixtures.token(), BigDecimal.ONE);
    CollectionItem paid = items.require(no);
    assertThat(paid.getStatus()).isEqualTo(ItemStatus.COMPLETED);
    assertThat(paid.getCompletedOn()).isNotNull();
    assertThat(changes.ofItem(paid.getId(), PageRequest.of(0, 20)).getContent())
        .anySatisfy(c -> assertThat(c.getField()).isEqualTo("status"))
        .anySatisfy(c -> assertThat(c.getField()).isEqualTo("currentHandler"));
    assertThatThrownBy(
            () -> dispose(CollectionsFixtures.HANDLER, no, "COORDINATE_FURTHER", Map.of()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("COMPLETED");
  }

  @Test
  void aDirectPaymentInvoiceWithoutReceivableIsNotListed() {
    String no = fx.directPaymentInvoice();
    assertThat(items.find(no)).isEmpty();
  }

  @Test
  void theDailyRefreshJobListsTheLedgerAndReportsItsCounts() {
    String no = fx.listedMotor().getInvoiceNo();
    var run = jobs.run(WorklistRefreshJob.JOB_NAME, JobTrigger.MANUAL);
    assertThat(run.getMessage()).contains("listed", "assigned by rule");
    assertThat(items.find(no)).isPresent();
    var page =
        worklist.search(
            fx.company(),
            WorklistFilter.of(ItemStatus.OPEN).with(new Scope("CBG", null, null, null, null)),
            PageRequest.of(0, 5));
    assertThat(page.getTotalElements()).isPositive();
    assertThat(worklist.totals(fx.company(), WorklistFilter.of(ItemStatus.OPEN), GroupBy.CLIENT))
        .anySatisfy(t -> assertThat(t.key()).isEqualTo(BookingFixtures.CLIENT));
    assertThat(worklist.aging(fx.company())).isNotEmpty();
  }

  @Test
  void aCheckPickupDispositionReachesTheCashieringPickupQueue() {
    CollectionItem item = fx.listedMotor();
    Map<String, String> details =
        Map.of(
            "pickupDate", LocalDate.now().plusDays(2).toString(),
            "pickupAddress", "21 Paseo de Roxas",
            "amount", "1500.00",
            "checkNo", "CHK-1");
    PrDisposition d =
        dispose(CollectionsFixtures.HANDLER, item.getInvoiceNo(), "FOR_CHECK_PICKUP", details)
            .get(0);
    String key = "PU:" + item.getInvoiceNo() + ":" + d.getId();

    assertThat(d.getOutboxId()).isNotNull();
    assertThat(ready.feeds()).contains(OutboxService.CHECK_PICKUP);
    assertThat(feed.transport()).isEqualTo("IN_APP");
    assertThat(outbox.pending(fx.company(), OutboxService.CHECK_PICKUP))
        .anySatisfy(
            i -> {
              assertThat(i.key()).isEqualTo(key);
              assertThat(i.fields()).containsEntry("amount", "1500.00");
            });

    as.run("cashier", () -> pickups.importPending(fx.company()));
    Long queued =
        jdbc.queryForObject(
            "select count(*) from csh_pickup_request where collection_ref = ?", Long.class, key);
    assertThat(queued).isEqualTo(1L);

    feed.acknowledge(fx.company(), OutboxService.CHECK_PICKUP, List.of(key));
    assertThat(outbox.pending(fx.company(), OutboxService.CHECK_PICKUP))
        .noneSatisfy(i -> assertThat(i.key()).isEqualTo(key));
    assertThat(items.require(item.getInvoiceNo()).getDispositionCode())
        .isEqualTo("FOR_CHECK_PICKUP");
  }

  @Test
  void aDpDispositionReachesTheCommissionDpListAndASecondDispositionWithdrawsIt() {
    CollectionItem first = fx.listedMotor();
    PrDisposition dp =
        dispose(CollectionsFixtures.HANDLER, first.getInvoiceNo(), "DP_PR_FOR_REVERSAL", Map.of())
            .get(0);
    String key = "DP:" + first.getInvoiceNo() + ":" + dp.getId();
    assertThat(items.require(first.getInvoiceNo()).getTaggingOwner())
        .isEqualTo(TaggingOwner.OPERATIONS);
    assertThat(outbox.pending(fx.company(), OutboxService.DP_LIST))
        .anySatisfy(i -> assertThat(i.key()).isEqualTo(key));

    as.run("commrec", () -> dpLists.pull(fx.company()));
    assertThat(dpItems.findByInvoiceNoOrderByIdDesc(first.getInvoiceNo())).isNotEmpty();
    feed.acknowledge(fx.company(), OutboxService.DP_LIST, List.of(key));

    CollectionItem second = fx.listedMotor();
    PrDisposition cwt =
        dispose(
                CollectionsFixtures.HANDLER,
                second.getInvoiceNo(),
                "PR2307_FOR_REVERSAL",
                Map.of("path", "CERTIFICATE", "certificateNo", "2307-T-1"))
            .get(0);
    String cwtKey = "CWT:" + second.getInvoiceNo() + ":" + cwt.getId();
    assertThat(outbox.pending(fx.company(), OutboxService.CWT2307))
        .anySatisfy(
            i -> {
              assertThat(i.key()).isEqualTo(cwtKey);
              assertThat(i.fields()).containsEntry("Certificate no", "2307-T-1");
            });
    dispose(CollectionsFixtures.HANDLER, second.getInvoiceNo(), "COORDINATE_FURTHER", Map.of());
    assertThat(outbox.pending(fx.company(), OutboxService.CWT2307))
        .noneSatisfy(i -> assertThat(i.key()).isEqualTo(cwtKey));
    assertThat(dispositions.ofItem(second.getId()))
        .hasSize(2)
        .last()
        .satisfies(d -> assertThat(d.getSupersededBy()).isNotNull());
    assertThat(timeline.timeline(second.getInvoiceNo()))
        .extracting(e -> e.kind())
        .contains("DISPOSITION", "HANDOFF", "LEDGER", "ASSIGNMENT");
  }

  @Test
  void dispositionRulesAreEnforced() {
    CollectionItem item = fx.listedMotor();
    String no = item.getInvoiceNo();
    assertThatThrownBy(() -> dispose("ao", no, "NO_POLICY_NUMBER", Map.of()))
        .hasMessageContaining("reserved");
    assertThat(dispose("proc", no, "NO_POLICY_NUMBER", Map.of())).hasSize(1);
    assertThatThrownBy(() -> dispose(CollectionsFixtures.HANDLER, no, "FOR_CHECK_PICKUP", Map.of()))
        .hasMessageContaining("pick-up date");
    assertThatThrownBy(
            () ->
                dispose(
                    CollectionsFixtures.HANDLER,
                    no,
                    "PR2307_FOR_REVERSAL",
                    Map.of("path", "CERTIFICATE")))
        .hasMessageContaining("certificate");
    assertThatThrownBy(() -> dispose(CollectionsFixtures.HANDLER, no, "NOT_A_CODE", Map.of()))
        .isInstanceOf(RuntimeException.class);
    CollectionItem other = fx.listedMotor();
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        dispositions.record(
                            fx.company(),
                            new DispositionCommand(
                                List.of(no, other.getInvoiceNo()),
                                "COORDINATE_FURTHER",
                                null,
                                null,
                                Map.of()))))
        .hasMessageContaining("CLX_BULK_UPDATE");
    List<PrDisposition> bulk =
        as.run(
            CollectionsFixtures.HANDLER,
            () ->
                dispositions.record(
                    fx.company(),
                    new DispositionCommand(
                        List.of(no, other.getInvoiceNo()),
                        "COORDINATE_FURTHER",
                        "Bulk",
                        null,
                        Map.of())));
    assertThat(bulk).hasSize(2).allSatisfy(d -> assertThat(d.getBulkRef()).startsWith("CLXBU-"));
  }

  @Test
  void effortsDetailsAndTheEditLock() {
    CollectionItem item = fx.listedMotor();
    String no = item.getInvoiceNo();
    as.run(
        CollectionsFixtures.HANDLER,
        () ->
            efforts.log(
                fx.company(),
                new EffortCommand(List.of(no), "CALL", null, "PHONE", "Treasury", "Promised")));
    as.run(
        CollectionsFixtures.HANDLER,
        () -> efforts.updateDetails(fx.company(), no, "Pays on the 30th", "B"));
    CollectionItem worked = items.require(no);
    assertThat(worked.getLastEffortCode()).isEqualTo("CALL");
    assertThat(worked.getCategory()).isEqualTo("B");
    assertThat(efforts.ofItem(item.getId())).hasSize(1);
    assertThatThrownBy(
            () ->
                as.run(
                    CollectionsFixtures.HANDLER,
                    () -> efforts.updateDetails(fx.company(), no, null, "D")))
        .hasMessageContaining("A, B or C");

    var mine = as.run(CollectionsFixtures.HANDLER, () -> locks.acquire(no));
    assertThat(mine.mine()).isTrue();
    var theirs = as.run("mkthandler", () -> locks.acquire(no));
    assertThat(theirs.mine()).isFalse();
    assertThat(theirs.editingBy()).isEqualTo(CollectionsFixtures.HANDLER);
    assertThatThrownBy(() -> dispose("mkthandler", no, "COORDINATE_FURTHER", Map.of()))
        .hasMessageContaining("is editing");
    as.run(CollectionsFixtures.HANDLER, () -> locks.release(no));
    assertThat(dispose("mkthandler", no, "COORDINATE_FURTHER", Map.of())).hasSize(1);
  }

  @Test
  void reassignmentIsPreviewedAppliedAndATemporaryOneReverts() {
    CollectionItem item = fx.listedMotor();
    String no = item.getInvoiceNo();
    ItemSelection selection = new ItemSelection(List.of(no), null);
    assertThat(as.run(CollectionsFixtures.LEAD, () -> assignments.preview(fx.company(), selection)))
        .satisfies(s -> assertThat(s.total()).isEqualTo(1));
    LocalDate today = LocalDate.now();
    as.run(
        CollectionsFixtures.LEAD,
        () ->
            assignments.reassign(
                fx.company(),
                selection,
                new Reassign("mkthandler", AssignmentKind.TEMPORARY, today.plusDays(1), "Leave")));
    assertThat(items.require(no).getCurrentHandler()).isEqualTo("mkthandler");
    assertThatThrownBy(
            () ->
                as.run(
                    CollectionsFixtures.LEAD,
                    () ->
                        assignments.reassign(
                            fx.company(),
                            selection,
                            new Reassign("uw", AssignmentKind.PERMANENT, null, "Wrong user"))))
        .hasMessageContaining("not an active collection handler");

    as.run(CollectionsFixtures.LEAD, () -> defaults.revertExpired(fx.company(), today.plusDays(3)));
    assertThat(items.require(no).getCurrentHandler()).isEqualTo(CollectionsFixtures.HANDLER);
    assertThat(assignments.history(item.getId()))
        .extracting(a -> a.getKind())
        .containsSubsequence(AssignmentKind.REVERT, AssignmentKind.TEMPORARY, AssignmentKind.RULE);
  }

  @Test
  void assignmentRulesAreMaintainedAndValidated() {
    String name = "Test rule " + BookingFixtures.token();
    var rule =
        as.run(
            CollectionsFixtures.LEAD,
            () ->
                rules.create(
                    fx.company(),
                    new Details(
                        5000,
                        name,
                        new RuleCriteria("ZZ-" + name, null, null, null, null, 30, 60),
                        "mkthandler")));
    assertThat(rules.list(fx.company())).anySatisfy(r -> assertThat(r.getName()).isEqualTo(name));
    as.run(CollectionsFixtures.LEAD, () -> rules.activate(rule.getId(), false));
    assertThatThrownBy(
            () ->
                as.run(
                    CollectionsFixtures.LEAD,
                    () ->
                        rules.update(
                            rule.getId(),
                            new Details(
                                5000,
                                name,
                                new RuleCriteria(null, null, null, null, null, 60, 30),
                                "mkthandler"))))
        .hasMessageContaining("range");
  }

  @Test
  void aDirectPaymentReturnedByTheInsurerReopensTheAccount() {
    CollectionItem item = fx.listedMotor();
    String no = item.getInvoiceNo();
    fx.pay(no, "APP:" + BookingFixtures.token(), BigDecimal.ONE);
    assertThat(items.require(no).getStatus()).isEqualTo(ItemStatus.COMPLETED);

    String run =
        as.run(
            "commrec",
            () ->
                feed.send(
                    fx.company(),
                    "COLLECTION_DP_RETURNED",
                    List.of(
                        new FeedItem(
                            "DPRET:" + BookingFixtures.token(),
                            Map.of("Invoice No.", no, "Reason", "NOT_PAID", "Comment", "Late")),
                        new FeedItem(
                            "DPRET:" + BookingFixtures.token(),
                            Map.of("Invoice No.", "BI-NOT-LISTED")))));
    assertThat(run).startsWith("FIR-");
    CollectionItem reopened = items.require(no);
    assertThat(reopened.getStatus()).isEqualTo(ItemStatus.OPEN);
    assertThat(reopened.getDispositionCode()).isEqualTo("DP_RETURNED");
    assertThat(timeline.timeline(no)).anySatisfy(e -> assertThat(e.kind()).isEqualTo("INBOX"));
  }
}
