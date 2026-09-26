package com.iortatechnxt.brokerverse.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.cashiering.CashFixtures;
import com.iortatechnxt.brokerverse.cashiering.domain.CollectorRequest;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.service.CollectorRequestService;
import com.iortatechnxt.brokerverse.cashiering.service.CollectorRequestService.Acceptance;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.service.CollectionsWorkCountSource.WorkCount;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.ApplicationRequest;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.UnappliedDisposition;
import com.iortatechnxt.brokerverse.collections.unapplied.service.ApplicationFileJob;
import com.iortatechnxt.brokerverse.collections.unapplied.service.ApplicationFileService;
import com.iortatechnxt.brokerverse.collections.unapplied.service.ApplicationRequestService;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedDispositionService;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedDispositionService.DispositionInput;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedRules;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedWorkCounts;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedWorklistService;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedWorklistService.CollectorFilter;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedWorklistService.CollectorRow;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DroppedFile;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedEvent;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/**
 * The collector side of unapplied payments (wave C1-C, BRCLXN.030-048): the list read from
 * Cashiering, collector dispositions with the invoice rules, the application request that
 * Cashiering accepts and applies (the invoice is paid), the status view and history, the daily "For
 * Application To Invoice" file, the home tiles and the reports.
 */
@IntegrationTest
class CollectionsUnappliedIT {

  private static final String HANDLER = CollectionsFixtures.HANDLER;
  private static final String APPLY = "FOR_APPLICATION_TO_INVOICE";

  @Autowired private CollectionsFixtures fx;
  @Autowired private CashFixtures cash;
  @Autowired private UnappliedWorklistService worklist;
  @Autowired private UnappliedDispositionService dispositions;
  @Autowired private ApplicationRequestService requests;
  @Autowired private ApplicationFileService files;
  @Autowired private ApplicationFileJob fileJob;
  @Autowired private UnappliedRules rules;
  @Autowired private UnappliedWorkCounts tiles;
  @Autowired private CollectorRequestService cashieringRequests;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;

  private Unapplied unmatched(BigDecimal amount) {
    return cash.pay("UNKNOWN-" + System.nanoTime(), amount).unapplied();
  }

  private static CollectorFilter text(String q) {
    return new CollectorFilter(q, null, null, null, null, null, null, null, null, null);
  }

  private UnappliedDisposition dispose(String ref, String code, String invoiceNo, String amount) {
    return as.run(
        HANDLER,
        () ->
            dispositions.dispose(
                fx.company(),
                ref,
                new DispositionInput(
                    code,
                    invoiceNo,
                    amount == null ? null : new BigDecimal(amount),
                    "Client call")));
  }

  @Test
  void aCollectorApplicationIsAppliedByCashieringAndTheInvoiceIsPaid() {
    CollectionItem listed = fx.listedMotor();
    OpsInvoice invoice = cash.invoice(listed.getInvoiceNo());
    BigDecimal due = invoice.premiumBalance();
    Unapplied item = unmatched(due);

    CollectorRow row =
        as.run(
                HANDLER,
                () -> worklist.list(fx.company(), text(item.getReference()), PageRequest.of(0, 5)))
            .getContent()
            .get(0);
    assertThat(row.item().balance()).isEqualByComparingTo(due);
    assertThat(row.ageDays()).isZero();
    assertThat(row.disposition()).isNull();

    UnappliedDisposition d = dispose(item.getReference(), APPLY, listed.getInvoiceNo(), null);
    assertThat(d.getCashieringAction()).isEqualTo("APPLY_TO_INVOICE");
    ApplicationRequest sent = requests.get(d.getRequestId());
    assertThat(sent.getStatus()).isEqualTo(ApplicationRequest.Status.SENT);
    assertThat(sent.getCashieringRef()).startsWith("CRQ-");
    assertThat(sent.getPayment().payorName()).startsWith("Test Payor");
    assertThat(sent.getPayment().assuredName()).isEqualTo(invoice.getAssuredName());

    CollectorRequest queued =
        cashieringRequests
            .list(
                fx.company(),
                List.of(CollectorRequest.Status.QUEUED),
                sent.getCashieringRef(),
                PageRequest.of(0, 5))
            .getContent()
            .get(0);
    as.run(
        "cashier",
        () ->
            cashieringRequests.accept(
                queued.getId(), new Acceptance(null, null, null, null, null, null, true)));

    ApplicationRequest applied = requests.get(d.getRequestId());
    assertThat(applied.getStatus()).isEqualTo(ApplicationRequest.Status.APPLIED);
    assertThat(applied.getStatusMessage()).contains("executed");
    assertThat(cash.invoice(listed.getInvoiceNo()).getPaymentStatus())
        .isEqualTo(PaymentStatus.PAID);
    assertThat(as.run(HANDLER, () -> requests.refresh(applied.getId())).getStatus())
        .isEqualTo(ApplicationRequest.Status.APPLIED);

    List<UnappliedEvent> history = dispositions.history(fx.company(), item.getReference());
    assertThat(history)
        .extracting(UnappliedEvent::event)
        .contains("RECEIVED", "COLLECTOR_DISPOSITION", "REQUESTED", "APPLIED");
    assertThat(dispositions.of(fx.company(), item.getReference())).hasSize(1);
    assertThat(requests.ofItem(fx.company(), item.getReference())).hasSize(1);
    assertThat(
            requests.list(
                fx.company(),
                List.of(ApplicationRequest.Status.APPLIED),
                item.getReference(),
                PageRequest.of(0, 5)))
        .hasSize(1);

    DroppedFile file =
        as.run(HANDLER, () -> files.publish(fx.company(), LocalDate.now().plusDays(1)))
            .orElseThrow();
    assertThat(file.path()).contains(ApplicationFileService.FOLDER).endsWith(".txt");
    assertThat(file.path()).endsWith(requests.get(d.getRequestId()).getFileRunNo());
    assertThat(files.publish(fx.company(), LocalDate.now().plusDays(1))).isEmpty();
    assertThat(fileJob.name()).isEqualTo("CLX_APPLICATION_FILE");
    assertThat(fileJob.execute(LocalDate.now()).message()).contains("file(s)");
  }

  @Test
  void theInvoiceRulesAndTheAmountAreChecked() {
    Unapplied item = unmatched(new BigDecimal("120.00"));
    String ref = item.getReference();
    assertThat(rules.invoicePattern()).contains("BI-");
    assertThat(rules.active())
        .extracting(UnappliedRules.Rule::code)
        .contains(APPLY, "COORDINATE_FURTHER");
    assertThatThrownBy(() -> dispose(ref, APPLY, null, null))
        .extracting("code")
        .isEqualTo("CLX_INVOICE_REQUIRED");
    assertThatThrownBy(() -> dispose(ref, APPLY, "X-123", null))
        .extracting("code")
        .isEqualTo("CLX_INVOICE_FORMAT");
    assertThatThrownBy(() -> dispose(ref, APPLY, "BI-NO-SUCH-" + System.nanoTime(), null))
        .extracting("code")
        .isEqualTo("CLX_INVOICE_UNKNOWN");
    assertThatThrownBy(() -> dispose(ref, "COORDINATE_FURTHER", null, "500.00"))
        .extracting("code")
        .isEqualTo("CLX_UNAPPLIED_AMOUNT");
    assertThatThrownBy(() -> dispose(ref, "NO_SUCH_VALUE", null, null))
        .isInstanceOf(RuntimeException.class);
    assertThatThrownBy(() -> dispose("UNP-NO-SUCH", "COORDINATE_FURTHER", null, null))
        .isInstanceOf(RuntimeException.class);

    UnappliedDisposition note = dispose(ref, "COORDINATE_FURTHER", null, null);
    assertThat(note.getRequestId()).isNull();
    UnappliedDisposition refund = dispose(ref, "FOR_REFUND", null, "100.00");
    assertThat(requests.get(refund.getRequestId()).getStatus())
        .isEqualTo(ApplicationRequest.Status.SENT);

    CollectorFilter byDisposition =
        new CollectorFilter(ref, null, null, null, null, null, null, null, null, "FOR_REFUND");
    assertThat(worklist.list(fx.company(), byDisposition, PageRequest.of(0, 5))).hasSize(1);
    CollectorFilter none =
        new CollectorFilter(
            ref,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            UnappliedWorklistService.NO_DISPOSITION);
    assertThat(worklist.list(fx.company(), none, PageRequest.of(0, 5))).isEmpty();
    CollectorFilter segment =
        new CollectorFilter(ref, null, null, null, null, null, 0, 30, "NO-SUCH-SEGMENT", null);
    assertThat(worklist.list(fx.company(), segment, PageRequest.of(0, 5))).isEmpty();
    CollectorFilter old =
        new CollectorFilter(ref, null, null, "UNAPPLIED", null, null, 5, null, null, null);
    assertThat(worklist.list(fx.company(), old, PageRequest.of(0, 5))).isEmpty();
    assertThat(dispositions.history(fx.company(), ref))
        .extracting(UnappliedEvent::event)
        .contains("COLLECTOR_DISPOSITION", "REQUESTED");
  }

  @Test
  void theHomeTilesAndReportsServeTheUnappliedHandlers() {
    List<WorkCount> counts = as.run(HANDLER, () -> tiles.counts(fx.company(), HANDLER));
    assertThat(counts).extracting(WorkCount::key).containsExactly("unapplied", "unappliedRequests");
    assertThat(as.run("proc", () -> tiles.counts(fx.company(), "proc"))).isEmpty();

    Map<String, String> params =
        Map.of(
            "companyId",
            String.valueOf(fx.company()),
            "from",
            LocalDate.now().minusDays(1).toString(),
            "to",
            LocalDate.now().plusDays(1).toString());
    for (String code : List.of("CLX-APPLICATION-TO-INVOICE", "CLX-UNAPPLIED-DISPOSITIONS")) {
      assertThat(as.run(HANDLER, () -> reports.export(code, params, ExportFormat.CSV)).content())
          .isNotEmpty();
    }
  }
}
