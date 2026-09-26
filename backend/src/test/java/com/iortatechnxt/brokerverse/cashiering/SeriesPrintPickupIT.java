package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PickupStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.OrAmounts;
import com.iortatechnxt.brokerverse.cashiering.domain.PickupRequest;
import com.iortatechnxt.brokerverse.cashiering.domain.PickupRequest.PickupDetails;
import com.iortatechnxt.brokerverse.cashiering.domain.PrintBatch;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt.ReceiptTender;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction.Reason;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptSeries;
import com.iortatechnxt.brokerverse.cashiering.service.BatchPrintService;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService.OrIssue;
import com.iortatechnxt.brokerverse.cashiering.service.PickupService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptActionService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSearchService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSearchService.ReceiptCriteria;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSeriesService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSeriesService.SeriesRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Receipt series (CSHID.006/015), Head Office ORs, receipt search (CSHID.010), batch printing
 * (CSHID.019), Certificate of Payment and the check pick-up queue (CSHID.009).
 */
@IntegrationTest
class SeriesPrintPickupIT {

  @Autowired private CashFixtures fx;
  @Autowired private ReceiptSeriesService series;
  @Autowired private CashReceiptService receipts;
  @Autowired private ReceiptSearchService search;
  @Autowired private BatchPrintService printing;
  @Autowired private PickupService pickups;
  @Autowired private ReceiptActionService actions;
  @Autowired private FlowInService flowIn;
  @Autowired private ApprovalInboxService inbox;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private Receipt ar(Long branch) {
    return as.run(
        "cashier",
        () ->
            receipts.issueAr(
                new CashReceiptService.ArIssue(
                    fx.company(),
                    branch,
                    "REFUND",
                    LocalDate.now(),
                    "INS-LAC",
                    "LAC Refund",
                    null,
                    null,
                    "PHP",
                    new BigDecimal("10.00"),
                    new ReceiptTender(
                        PaymentMode.CASH,
                        null,
                        null,
                        null,
                        null,
                        ReceiptSource.OTC,
                        null,
                        null,
                        null))));
  }

  @Test
  void aSmallSeriesIsAuthorizedWithFourEyesWarnsAndRunsOut() {
    Long dvo = fx.branch("DVO");
    String prefix = "AR-T" + (System.nanoTime() % 100000) + "-";
    ReceiptSeries created =
        as.run(
            "cashtl",
            () ->
                series.create(
                    new SeriesRequest(
                        fx.company(), dvo, ReceiptKind.AR, prefix, 1, 2, "ATP-T", 1)));
    assertThat(created.getRecordStatus().name()).isEqualTo("PENDING_AUTHORIZATION");
    assertThat(as.run("approver", () -> inbox.inbox(fx.company())))
        .anyMatch(p -> p.reference().startsWith(prefix));
    assertThatThrownBy(() -> as.run("cashtl", () -> series.authorize(created.getId())))
        .extracting("code")
        .isEqualTo("MAKER_CHECKER_VIOLATION");
    as.run("approver", () -> series.authorize(created.getId()));

    assertThat(ar(dvo).getReceiptNo()).isEqualTo(prefix + "1");
    assertThat(ar(dvo).getReceiptNo()).isEqualTo(prefix + "2");
    assertThatThrownBy(() -> ar(dvo)).extracting("code").isEqualTo("RECEIPT_SERIES_DEPLETED");
    Long alerts =
        jdbc.queryForObject(
            "select count(*) from alt_alert where dedup_key = ?",
            Long.class,
            "RECEIPT_SERIES_LOW:" + created.getId());
    assertThat(alerts).isEqualTo(1);
    assertThat(series.list(fx.company()))
        .extracting(ReceiptSeries::getId)
        .contains(created.getId());
    as.run("cashtl", () -> series.update(created.getId(), "ATP-T2", 5, 1));
    assertThat(series.get(created.getId()).getRecordStatus().name())
        .isEqualTo("PENDING_AUTHORIZATION");
    as.run("cashtl", () -> series.deactivate(created.getId()));
    assertThatThrownBy(
            () ->
                as.run(
                    "cashtl",
                    () ->
                        series.create(
                            new SeriesRequest(
                                fx.company(), dvo, ReceiptKind.OR, prefix + "O", 1, 9, null, 1))))
        .extracting("code")
        .isEqualTo("OR_HEAD_OFFICE_ONLY");
  }

  @Test
  void anOrIsHeadOfficeOnlyAndReceiptsAreFoundPrintedAndCertified() {
    OrIssue fee =
        new OrIssue(
            fx.company(),
            fx.branch("CEB"),
            "SERVICE_FEE",
            LocalDate.now(),
            "CL-2026-000003",
            "Pacific Harbor Logistics Inc.",
            "PHP",
            List.of(
                CashReceiptService.line(
                    null,
                    null,
                    new OrAmounts(
                        new BigDecimal("1000.00"),
                        new BigDecimal("120.00"),
                        new BigDecimal("20.00")),
                    "Consultancy")),
            new ReceiptTender(
                PaymentMode.CHECK, "C1", "BDO", null, null, ReceiptSource.OTC, null, null, null),
            false);
    assertThatThrownBy(() -> as.run("cashier", () -> receipts.issueOr(fee)))
        .extracting("code")
        .isEqualTo("OR_HEAD_OFFICE_ONLY");
    Receipt or =
        as.run(
            "cashier",
            () ->
                receipts.issueOr(
                    new OrIssue(
                        fx.company(),
                        null,
                        fee.orType(),
                        fee.receiptDate(),
                        fee.payorCode(),
                        fee.payorName(),
                        fee.currency(),
                        fee.lines(),
                        fee.tender(),
                        false)));
    assertThat(or.getAmount()).isEqualByComparingTo("1100.00");
    assertThat(or.getJournalBatchNo()).isNotNull();

    OpsInvoice invoice = fx.motorInvoice();
    Receipt ar = fx.pay(invoice.getInvoiceNo(), new BigDecimal("200.00")).receipt();
    assertThat(
            as.run(
                "cashier",
                () ->
                    search.search(
                        new ReceiptCriteria(
                            fx.company(),
                            null,
                            null,
                            invoice.getInvoiceNo(),
                            null,
                            null,
                            null,
                            null,
                            null,
                            invoice.getPolicyNo(),
                            null,
                            ReceiptKind.AR,
                            null),
                        Pageable.ofSize(10))))
        .extracting(Receipt::getId)
        .containsExactly(ar.getId());
    assertThat(
            as.run(
                "cashier",
                () ->
                    search.search(
                        new ReceiptCriteria(
                            fx.company(),
                            or.getReceiptNo(),
                            null,
                            null,
                            "harbor",
                            null,
                            new BigDecimal("1100.00"),
                            LocalDate.now().minusDays(1),
                            LocalDate.now(),
                            null,
                            null,
                            null,
                            null),
                        Pageable.ofSize(10))))
        .hasSize(1);

    Receipt third = fx.pay("UNKNOWN-P-" + System.nanoTime(), new BigDecimal("10.00")).receipt();
    PrintBatch batch =
        as.run(
            "cashier",
            () ->
                printing.print(
                    fx.company(), List.of(ar.getId(), or.getId(), third.getId()), "test"));
    assertThat(batch.getPrintedCount()).isEqualTo(3);
    assertThat(batch.document()).isNotEmpty();
    assertThatThrownBy(() -> as.run("cashier", () -> printing.retry(batch.getId())))
        .extracting("code")
        .isEqualTo("PRINT_NOTHING_FAILED");
    assertThat(printing.get(batch.getId()).getLines()).hasSize(3);
    assertThat(
            as.run(
                "cashier",
                () -> printing.certificateOfPayment(ar.getId(), invoice.getPolicyNo(), "CBG-NCR")))
        .isNotEmpty();
    assertThat(printing.list(fx.company(), Pageable.ofSize(5))).isNotEmpty();
  }

  @Test
  void checksForPickUpAreQueuedAndTheirArsPrintedWhenDue() {
    String ref = "COL-T-" + System.nanoTime();
    PickupRequest due =
        as.run(
            "cashier",
            () ->
                pickups.create(
                    fx.company(),
                    fx.ho(),
                    new PickupDetails(
                        ref,
                        "NO-SUCH-" + ref,
                        null,
                        "Pick-up Payor",
                        null,
                        LocalDate.now(),
                        "Collection",
                        new BigDecimal("700.00"),
                        "PHP",
                        "CHK1",
                        "BDO")));
    PickupRequest later =
        as.run(
            "cashier",
            () ->
                pickups.create(
                    fx.company(),
                    fx.ho(),
                    new PickupDetails(
                        ref + "L",
                        "NO-SUCH-" + ref,
                        null,
                        "Pick-up Payor",
                        null,
                        LocalDate.now().plusDays(10),
                        "Collection",
                        new BigDecimal("700.00"),
                        "PHP",
                        "CHK2",
                        "BDO")));
    assertThatThrownBy(
            () ->
                as.run(
                    "cashier",
                    () ->
                        pickups.create(
                            fx.company(),
                            fx.ho(),
                            new PickupDetails(
                                ref,
                                "X",
                                null,
                                "P",
                                null,
                                LocalDate.now(),
                                "C",
                                BigDecimal.ONE,
                                "PHP",
                                null,
                                null))))
        .extracting("code")
        .isEqualTo("PICKUP_DUPLICATE");

    List<Long> printed =
        as.run("cashier", () -> pickups.printArs(List.of(due.getId(), later.getId())));
    assertThat(printed).hasSize(1);
    assertThat(
            pickups.list(fx.company(), PickupStatus.AR_PRINTED, null, null, Pageable.ofSize(500)))
        .extracting(PickupRequest::getCollectionRef)
        .contains(ref);
    assertThatThrownBy(() -> as.run("cashier", () -> pickups.printArs(List.of(later.getId()))))
        .extracting("code")
        .isEqualTo("PICKUP_NOTHING_DUE");
    as.run("cashier", () -> pickups.cancel(later.getId()));
    assertThat(as.run("cashier", () -> pickups.importPending(fx.company()))).isZero();

    String csv =
        "company,collectionRef,reference,clientCode,payorName,pickupDate,amount,currency,checkNo,checkBank,requestor\n"
            + "FVI,"
            + ref
            + "F,ARN-X,CL-2026-000001,Feed Payor,2026-10-01,900.00,PHP,CHK3,BDO,Collection\n";
    var run =
        as.run(
            "admin",
            () ->
                flowIn.upload(
                    PickupService.FEED,
                    new FlowInFile("pickup.csv", csv.getBytes(StandardCharsets.UTF_8))));
    assertThat(run.getOkCount()).isEqualTo(1);
    assertThat(actions.ofReceipt(printed.get(0))).isEmpty();
    as.run(
        "cashier",
        () -> actions.requestCancel(printed.get(0), new Reason("PRM_CHECK_NOT_PICKED_UP", null)));
  }
}
