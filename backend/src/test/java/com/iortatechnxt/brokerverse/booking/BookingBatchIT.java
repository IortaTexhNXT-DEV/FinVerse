package com.iortatechnxt.brokerverse.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.booking.domain.AutoBookRule;
import com.iortatechnxt.brokerverse.booking.domain.BatchRow;
import com.iortatechnxt.brokerverse.booking.domain.BatchRun;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceStatus;
import com.iortatechnxt.brokerverse.booking.domain.QueueEntry;
import com.iortatechnxt.brokerverse.booking.domain.QueueEntryRepository;
import com.iortatechnxt.brokerverse.booking.domain.QueueSource;
import com.iortatechnxt.brokerverse.booking.domain.QueueStatus;
import com.iortatechnxt.brokerverse.booking.service.BookingBatchJob;
import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService;
import com.iortatechnxt.brokerverse.booking.service.BookingQueueService;
import com.iortatechnxt.brokerverse.booking.service.BookingQueueService.EnqueueResult;
import com.iortatechnxt.brokerverse.booking.service.BookingRuleService;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.booking.service.BookingUploadHandler;
import com.iortatechnxt.brokerverse.booking.service.BookingWorkbenchService;
import com.iortatechnxt.brokerverse.booking.service.BookingWorkbenchService.Filter;
import com.iortatechnxt.brokerverse.booking.service.BookingWorkbenchService.Row;
import com.iortatechnxt.brokerverse.booking.service.BookingWorkbenchService.Tab;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/** Queue, batches with partial success, the end-of-day job, multi-year years, auto-book, upload. */
@IntegrationTest
class BookingBatchIT {

  private static final LocalDate CLOSED = LocalDate.of(2025, 12, 15);

  @Autowired private BookingFixtures fx;
  @Autowired private BookingQueueService queue;
  @Autowired private BookingService booking;
  @Autowired private BookingQueryService queries;
  @Autowired private BookingWorkbenchService workbench;
  @Autowired private BookingRuleService rules;
  @Autowired private BookingBatchJob job;
  @Autowired private BookingUploadHandler upload;
  @Autowired private QueueEntryRepository entries;
  @Autowired private AsUser as;

  private QueueEntry enqueue(Account account) {
    List<EnqueueResult> results =
        as.run(
            "proc",
            () -> queue.enqueue(fx.company(), List.of(account.getArn()), QueueSource.MANUAL));
    assertThat(results).singleElement().satisfies(r -> assertThat(r.queued()).isTrue());
    return entries.findByArnAndStatus(account.getArn(), QueueStatus.QUEUED).orElseThrow();
  }

  private List<Row> rows(Tab tab, String arn) {
    return workbench
        .rows(fx.company(), tab, new Filter(arn, null), PageRequest.of(0, 50))
        .getContent();
  }

  @Test
  void aConfirmedBatchBooksEachAccountOnItsOwnWithPartialSuccess() {
    Account a = fx.motor();
    Account b = fx.motor();
    Account c = fx.motor();
    QueueEntry ea = enqueue(a);
    QueueEntry eb = enqueue(b);
    QueueEntry ec = enqueue(c);
    assertThat(rows(Tab.QUEUED, a.getArn())).hasSize(1);
    assertThat(rows(Tab.READY, a.getArn())).isEmpty();
    as.run("proc", () -> queue.edit(ec.getId(), CLOSED, null));

    BatchRun run =
        as.run(
            "proc",
            () ->
                queue.confirmBatch(
                    fx.company(),
                    List.of(ea.getId(), eb.getId(), ec.getId()),
                    BookingFixtures.BOOKED_ON));

    assertThat(run.getRunNo()).startsWith("BB-2026-");
    assertThat(run.getBookedCount()).isEqualTo(2);
    assertThat(run.getFailedCount()).isEqualTo(1);
    assertThat(run.getRows())
        .filteredOn(r -> r.arn().equals(c.getArn()))
        .singleElement()
        .satisfies(r -> assertThat(r.outcome()).isEqualTo(BatchRow.FAILED));
    assertThat(queries.run(run.getRunNo()).getRows()).hasSize(3);
    assertThat(entries.findById(ea.getId()).orElseThrow().getStatus())
        .isEqualTo(QueueStatus.BOOKED);
    QueueEntry failed = entries.findById(ec.getId()).orElseThrow();
    assertThat(failed.getStatus()).isEqualTo(QueueStatus.FAILED);
    assertThat(failed.getLastError()).isNotBlank();
    assertThat(rows(Tab.FAILED, c.getArn())).hasSize(1);
    assertThat(rows(Tab.BOOKED, a.getArn())).hasSize(1);
    assertThat(workbench.counts(fx.company()).failed()).isPositive();

    enqueue(c);
    assertThat(entries.findById(ec.getId()).orElseThrow().getStatus())
        .isEqualTo(QueueStatus.REMOVED);
    BatchRun now =
        as.run(
            "proc",
            () -> queue.bookNow(fx.company(), List.of(c.getArn()), BookingFixtures.BOOKED_ON));
    assertThat(now.getBookedCount()).isEqualTo(1);
    assertThat(queries.invoicesForArn(c.getArn())).hasSize(1);
    assertThat(entries.findByArnAndStatus(c.getArn(), QueueStatus.QUEUED)).isEmpty();
  }

  @Test
  void queuedAccountsCanBeChangedRemovedOrTheBatchCancelled() {
    Account d = fx.motor();
    Account e = fx.motor();
    QueueEntry ed = enqueue(d);
    QueueEntry ee = enqueue(e);
    List<EnqueueResult> again =
        as.run(
            "proc",
            () ->
                queue.enqueue(
                    fx.company(), List.of(d.getArn(), "ARN-1900-000001"), QueueSource.MANUAL));
    assertThat(again)
        .extracting(EnqueueResult::message)
        .containsExactly("Already queued", "Unknown account, or already booked");

    QueueEntry edited =
        as.run("proc", () -> queue.edit(ed.getId(), BookingFixtures.BOOKED_ON, "MKT"));
    assertThat(edited.getCostCenter()).isEqualTo("MKT");
    assertThatThrownBy(() -> as.run("proc", () -> queue.edit(ed.getId(), null, "NOPE")))
        .extracting("code")
        .isEqualTo("INVALID_DIMENSION");
    assertThatThrownBy(
            () -> as.run("proc", () -> queue.edit(ed.getId(), LocalDate.now().plusDays(5), null)))
        .extracting("code")
        .isEqualTo("BOOKING_DATE_FUTURE");
    as.run("proc", () -> queue.remove(ee.getId()));
    assertThatThrownBy(() -> as.run("proc", () -> queue.remove(ee.getId())))
        .extracting("code")
        .isEqualTo("QUEUE_ENTRY_CLOSED");

    int removed = as.run("proc", () -> queue.cancelBatch(fx.company()));
    assertThat(removed).isPositive();
    assertThat(entries.findById(ed.getId()).orElseThrow().getStatus())
        .isEqualTo(QueueStatus.REMOVED);
    assertThatThrownBy(
            () -> as.run("proc", () -> queue.confirmBatch(fx.company(), List.of(ed.getId()), null)))
        .extracting("code")
        .isEqualTo("BATCH_EMPTY");
    assertThatThrownBy(() -> as.run("proc", () -> queue.bookNow(fx.company(), List.of(), null)))
        .extracting("code")
        .isEqualTo("BATCH_EMPTY");
  }

  @Test
  void theEndOfDayJobBooksTheQueueAndStartedMultiYearPolicyYears() {
    Account multi = fx.multiYear(LocalDate.of(2023, 10, 1), 3);
    BookedInvoice first =
        as.run(
            "proc",
            () ->
                booking.book(
                    multi.getArn(),
                    BookingOptions.of(BookingFixtures.BOOKED_ON, null),
                    BookingSource.INDIVIDUAL));
    List<BookedInvoice> schedule = queries.schedule(multi.getArn());
    assertThat(schedule)
        .extracting(BookedInvoice::getStatus)
        .containsExactly(InvoiceStatus.BOOKED, InvoiceStatus.SCHEDULED, InvoiceStatus.SCHEDULED);
    assertThat(schedule)
        .extracting(BookedInvoice::getPolicyNo)
        .allMatch(p -> p != null && p.startsWith("POL-"));
    assertThat(schedule)
        .extracting(BookedInvoice::getInceptionDate)
        .containsExactly(
            LocalDate.of(2023, 10, 1), LocalDate.of(2024, 10, 1), LocalDate.of(2025, 10, 1));
    assertThat(first.getTransactionNo()).isEqualTo("NB");

    Account queued = fx.motor();
    QueueEntry entry = enqueue(queued);
    job.execute(BookingFixtures.BOOKED_ON);

    assertThat(queries.schedule(multi.getArn()))
        .extracting(BookedInvoice::getStatus)
        .containsOnly(InvoiceStatus.BOOKED);
    assertThat(queries.invoicesForArn(multi.getArn()))
        .allSatisfy(
            i -> assertThat(i.commission()).isEqualByComparingTo(multi.getPremium().commission()));
    assertThat(entries.findById(entry.getId()).orElseThrow().getStatus())
        .isEqualTo(QueueStatus.BOOKED);
    assertThat(job.name()).isEqualTo("BOOKING_BATCH");
    assertThat(job.description()).isNotBlank();
    assertThat(job.cron()).isNotBlank();
    assertThat(queries.runs(fx.company(), PageRequest.of(0, 5)).getContent()).isNotEmpty();
  }

  @Test
  void autoBookRulesQueueIssuedAccountsForTheNextBatch() {
    AutoBookRule rule =
        as.run(
            "badmin",
            () ->
                rules.createAutoBookRule(
                    fx.company(),
                    new AutoBookRule.Criteria("MTR26", "RETAIL", true, "Test auto-book MTR26")));
    try {
      Account auto =
          fx.issued(BookingFixtures.spec("MTR26", "RETAIL", PaymentArrangement.VIA_BDOI));
      QueueEntry entry =
          entries.findByArnAndStatus(auto.getArn(), QueueStatus.QUEUED).orElseThrow();
      assertThat(entry.getSource()).isEqualTo(QueueSource.AUTO);
      Account manual = fx.motor();
      assertThat(entries.findByArnAndStatus(manual.getArn(), QueueStatus.QUEUED)).isEmpty();
      assertThat(rows(Tab.READY, manual.getArn()))
          .singleElement()
          .satisfies(r -> assertThat(r.status()).isEqualTo("POLICY_ISSUED"));
    } finally {
      as.run(
          "badmin",
          () ->
              rules.updateAutoBookRule(
                  rule.getId(),
                  new AutoBookRule.Criteria("MTR26", "RETAIL", false, "Test auto-book MTR26")));
    }
    assertThat(rules.autoBooks(fx.company(), "MTR26", "RETAIL")).isFalse();
  }

  @Test
  void theUploadBooksEachValidRow() {
    Account account = fx.motor();
    BulkContext context =
        new BulkContext(fx.company(), "BLK-TEST", BookingFixtures.BOOKED_ON, Map.of());
    BulkRow bad = new BulkRow(1, Map.of("ARN", "ARN-1900-000002", "Cost Center", "NOPE"));
    assertThat(upload.validate(bad, context)).isNotEmpty();
    BulkRow future = new BulkRow(2, Map.of("ARN", account.getArn(), "Booking Date", "2099-01-01"));
    assertThat(upload.validate(future, context)).isNotEmpty();
    BulkRow good = new BulkRow(3, Map.of("ARN", account.getArn()));
    assertThat(upload.validate(good, context)).isEmpty();
    String invoiceNo = as.run("proc", () -> upload.commit(good, context));
    assertThat(invoiceNo).startsWith("BI-HO-");
    assertThat(upload.validate(good, context)).isNotEmpty();
    assertThat(upload.code()).isEqualTo("BOOKING_UPLOAD");
    assertThat(upload.columns()).hasSize(3);
    assertThat(upload.duplicateKey(good)).isEqualTo(account.getArn());
    assertThat(upload.title()).isNotBlank();
    assertThat(upload.instructions()).isNotBlank();
    assertThat(upload.permission()).isEqualTo("BOOKING_PROCESS");
  }
}
