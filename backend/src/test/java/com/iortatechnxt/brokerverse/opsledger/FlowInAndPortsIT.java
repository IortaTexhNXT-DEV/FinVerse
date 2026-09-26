package com.iortatechnxt.brokerverse.opsledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.brokerclaims.feed.service.InAppClaimsFeed;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringPaymentReapplier;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringReceiptIssuer;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringUnappliedSink;
import com.iortatechnxt.brokerverse.collections.feed.service.InAppCollectionFeed;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.disbursement.service.DisbursementGatewayAdapter;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.RecordStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.RunStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInFeed;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.BookRates;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.opsledger.service.ExtractRepositoryService;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInService;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.DisbursementStatusChanged;
import com.iortatechnxt.brokerverse.opsledger.service.adapter.HandoffReceiptIssuer;
import com.iortatechnxt.brokerverse.opsledger.service.adapter.HandoffUnappliedSink;
import com.iortatechnxt.brokerverse.opsledger.service.adapter.LedgerPaymentReapplier;
import com.iortatechnxt.brokerverse.opsledger.service.adapter.ManualCollectionFeed;
import com.iortatechnxt.brokerverse.opsledger.service.adapter.ManualInsurerFileInbox;
import com.iortatechnxt.brokerverse.opsledger.service.adapter.RepositoryFileDrop;
import com.iortatechnxt.brokerverse.opsledger.service.port.ClaimsFeed;
import com.iortatechnxt.brokerverse.opsledger.service.port.CollectionFeed;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway;
import com.iortatechnxt.brokerverse.opsledger.service.port.EarlyIncentiveRules;
import com.iortatechnxt.brokerverse.opsledger.service.port.FeedItem;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DropContent;
import com.iortatechnxt.brokerverse.opsledger.service.port.InsurerFileInbox;
import com.iortatechnxt.brokerverse.opsledger.service.port.MarketingFeed;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReapplier;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedSink;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceEarlyIncentiveRules;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The flow-in framework, the default adapters of the Operations ports and which module bean serves
 * each port once all Operations modules are installed.
 */
@IntegrationTest
class FlowInAndPortsIT {

  private static final String MODULE = "TESTMOD";

  @Autowired private FlowInService flowIn;
  @Autowired private TestFlowInHandler handler;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TransactionTemplate tx;
  @Autowired private AsUser as;
  @Autowired private OpsLedgerFixtures fx;
  @Autowired private ReceiptIssuer activeReceiptIssuer;
  @Autowired private UnappliedSink activeUnappliedSink;
  @Autowired private PaymentReapplier activeReapplier;
  @Autowired private ObjectMapper json;
  @Autowired private EarlyIncentiveRules incentiveRules;
  @Autowired private ObjectProvider<MarketingFeed> marketingFeed;
  @Autowired private ObjectProvider<ClaimsFeed> claimsFeed;
  @Autowired private InvoiceLedgerQueryService ledgerQuery;
  private ReceiptIssuer receiptIssuer;
  private UnappliedSink unappliedSink;
  private PaymentReapplier reapplier;

  /** The default adapters stay the fallback while no module provides the ports. */
  @BeforeEach
  void defaultAdapters() {
    receiptIssuer = new HandoffReceiptIssuer(handoffs, json);
    unappliedSink = new HandoffUnappliedSink(handoffs, json);
    reapplier = new LedgerPaymentReapplier(ledgerQuery);
  }

  @Autowired private DisbursementGateway gateway;
  @Autowired private DisbursementQueueService queue;
  @Autowired private CollectionFeed collection;
  @Autowired private InsurerFileInbox inbox;
  @Autowired private FileDropPort fileDrop;
  @Autowired private ExtractRepositoryService extracts;
  @Autowired private HandoffService handoffs;
  @Autowired private InvoiceLedgerService ledger;
  @Autowired private BookRates rates;
  @Autowired private CapturedLedgerEvents events;

  @BeforeEach
  void testFeed() {
    jdbc.update(
        "insert into ops_flow_in_feed (code, name, partner_system, direction, transport, owner_module,"
            + " created_at, created_by) values (?, 'Test feed', 'TEST', 'INBOUND', 'MANUAL_UPLOAD',"
            + " 'TEST', now(), 'TEST') on conflict (code) do nothing",
        TestFlowInHandler.FEED);
  }

  private static FlowInFile file(String text) {
    return new FlowInFile("test.txt", text.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void aRunKeepsGoingPastFailedRecordsAndSkipsDuplicates() {
    String a = BookingFixtures.token();
    String b = BookingFixtures.token();
    FlowInRun first =
        as.run(
            "admin", () -> flowIn.upload(TestFlowInHandler.FEED, file(a + ";OK\n" + b + ";FAIL")));
    assertThat(first.getStatus()).isEqualTo(RunStatus.PARTIAL);
    assertThat(first.getOkCount()).isEqualTo(1);
    assertThat(first.getFailedCount()).isEqualTo(1);
    assertThat(first.getFileSha256()).hasSize(64);
    assertThat(handler.processed()).contains(a).doesNotContain(b);
    assertThat(flowIn.records(first.getId(), PageRequest.of(0, 10)).getContent())
        .extracting(r -> r.getIdempotencyKey() + ":" + r.getStatus())
        .containsExactlyInAnyOrder(a + ":" + RecordStatus.ACCEPTED, b + ":" + RecordStatus.FAILED);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from alt_alert where exception_code = ? and dedup_key = ?",
                Integer.class,
                FlowInService.FAILED_ALERT,
                "FLOWIN:" + first.getRunNo()))
        .isEqualTo(1);

    FlowInRun second =
        as.run("admin", () -> flowIn.upload(TestFlowInHandler.FEED, file(a + ";OK\n" + b + ";OK")));
    assertThat(second.getStatus()).isEqualTo(RunStatus.SUCCEEDED);
    assertThat(second.getDuplicateCount()).isEqualTo(1);
    assertThat(second.getOkCount()).isEqualTo(1);
    assertThat(flowIn.runs(TestFlowInHandler.FEED, PageRequest.of(0, 50)).getContent())
        .extracting(FlowInRun::getRunNo)
        .contains(first.getRunNo(), second.getRunNo());

    FlowInRun broken =
        as.run(
            "admin",
            () -> flowIn.upload(TestFlowInHandler.FEED, file(BookingFixtures.token() + ";BROKEN")));
    assertThat(broken.getStatus()).isEqualTo(RunStatus.FAILED);
    assertThat(broken.getErrorDetail()).contains("Broken file");
  }

  @Test
  void feedsAreConfigurableAndInactiveOrUnhandledFeedsAreRefused() {
    assertThat(flowIn.feeds())
        .extracting(f -> f.getCode())
        .contains("OPS_INVOICE_FEED", "COLLECTION_HOLD");
    assertThat(flowIn.hasHandler(TestFlowInHandler.FEED)).isTrue();
    assertThatThrownBy(() -> flowIn.upload("DISBURSEMENT_STATUS", file("x;OK")))
        .extracting("code")
        .isEqualTo("FLOW_IN_NO_HANDLER");
    as.run("admin", () -> flowIn.configure(TestFlowInHandler.FEED, "0 0 3 * * *", false));
    try {
      assertThatThrownBy(() -> flowIn.upload(TestFlowInHandler.FEED, file("x;OK")))
          .extracting("code")
          .isEqualTo("FLOW_IN_FEED_INACTIVE");
    } finally {
      as.run("admin", () -> flowIn.configure(TestFlowInHandler.FEED, "-", true));
    }
    assertThat(flowIn.feed(TestFlowInHandler.FEED).getCron()).isEqualTo("-");
  }

  @Test
  void everyPortHasItsModuleBeanAndOnlyParkedIntegrationsKeepTheirDefault() {
    // Cashiering provides the receipt, unapplied and re-application ports and remittance the early
    // incentive rules: their beans win over the defaults.
    assertThat(activeReceiptIssuer).isInstanceOf(CashieringReceiptIssuer.class);
    assertThat(activeUnappliedSink).isInstanceOf(CashieringUnappliedSink.class);
    assertThat(activeReapplier).isInstanceOf(CashieringPaymentReapplier.class);
    assertThat(incentiveRules).isInstanceOf(RemittanceEarlyIncentiveRules.class);
    // Collections serves the Collection feeds in-app (BRD-4, OQ01 answered).
    assertThat(collection).isInstanceOf(InAppCollectionFeed.class);
    // Disbursement (A1-DSB, OQ02) replaces the queue gateway, keeping the queue record.
    assertThat(gateway).isInstanceOf(DisbursementGatewayAdapter.class);
    // Parked integrations (OQ17, OQ22) keep the in-app defaults.
    assertThat(inbox).isInstanceOf(ManualInsurerFileInbox.class);
    assertThat(fileDrop).isInstanceOf(RepositoryFileDrop.class);
    assertThat(marketingFeed.getIfAvailable()).isNull();
    // Claims serves the claims special remittance feed in-app (BRD-7, OQ46).
    assertThat(claimsFeed.getIfAvailable()).isInstanceOf(InAppClaimsFeed.class);
    assertThat(inbox.pending(fx.company(), "INS-MGIC", "PRODUCTION")).isEmpty();
    assertThat(inbox.transport()).isEqualTo("MANUAL_UPLOAD");
    assertThat(collection.pending(fx.company(), "COLLECTION_HOLD")).isEmpty();
  }

  @Test
  void everyInboundFeedByUploadHasItsHandler() {
    // Collections serves three COLLECTION_* feeds in-app (V1000); their uploads stay a fallback.
    assertThat(flowIn.feeds())
        .filteredOn(f -> f.getTransport() == FlowInEnums.Transport.IN_APP)
        .extracting(FlowInFeed::getCode)
        .contains(
            "COLLECTION_CWT2307",
            "COLLECTION_DP_LIST",
            "COLLECTION_CHECK_PICKUP",
            "COLLECTION_DP_RETURNED",
            "COLLECTION_REFUND")
        .doesNotContain("COLLECTION_HOLD", "COLLECTION_SPECIAL_REMIT");
    assertThat(flowIn.feeds())
        .filteredOn(f -> f.getDirection() == FlowInEnums.Direction.INBOUND)
        .filteredOn(
            f ->
                f.getTransport() == FlowInEnums.Transport.MANUAL_UPLOAD
                    || f.getCode().startsWith("COLLECTION_"))
        .filteredOn(f -> !f.getCode().equals(TestFlowInHandler.FEED))
        .extracting(FlowInFeed::getCode)
        .containsExactlyInAnyOrder(
            "COLLECTION_CHECK_PICKUP",
            "COLLECTION_CWT2307",
            "COLLECTION_COMMISSION_PAYMENT",
            "COLLECTION_HOLD",
            "COLLECTION_SPECIAL_REMIT",
            "COLLECTION_DP_LIST",
            "INSURER_REMIT_OR",
            "INSURER_PRODUCTION",
            "INSURER_DP_RESPONSE")
        .allSatisfy(code -> assertThat(flowIn.hasHandler(code)).as(code).isTrue());
    assertThat(flowIn.hasHandler("OPS_INVOICE_FEED")).isFalse();
    assertThat(flowIn.hasHandler("DISBURSEMENT_STATUS")).isFalse();
  }

  @Test
  void receiptsAndUnappliedItemsAreHandedOverUntilCashieringIsActive() {
    String ref = "SB-" + BookingFixtures.token();
    ReceiptIssuer.IssuedReceipt receipt =
        as.run(
            "remit",
            () ->
                tx.execute(
                    s ->
                        receiptIssuer.issueOfficialReceipt(
                            new ReceiptIssuer.ReceiptRequest(
                                fx.company(),
                                "COMMISSION",
                                new ReceiptIssuer.Payee("INS-MGIC", "MGIC"),
                                "PHP",
                                LocalDate.of(2026, 9, 18),
                                List.of(
                                    new ReceiptIssuer.ReceiptLine(
                                        "BI-X",
                                        "INS-MGIC",
                                        new BigDecimal("1000.00"),
                                        new BigDecimal("120.00"),
                                        new BigDecimal("100.00"),
                                        "Commission")),
                                new ReceiptIssuer.Source(MODULE, ref, null, null)))));
    assertThat(receipt.status()).isEqualTo(ReceiptIssuer.Status.DEFERRED);
    assertThat(receipt.receiptNo()).isNull();

    UnappliedSink.UnappliedHandle handle =
        as.run(
            "adjust",
            () ->
                tx.execute(
                    s ->
                        unappliedSink.create(
                            new UnappliedSink.UnappliedRequest(
                                fx.company(),
                                "ADJUSTMENT",
                                new UnappliedSink.Party(BookingFixtures.CLIENT, "MKT"),
                                "PHP",
                                new BigDecimal("500.00"),
                                null,
                                "REFUND",
                                new UnappliedSink.Source(MODULE, ref, "Excess")))));
    assertThat(handle.status()).isEqualTo(UnappliedSink.Status.DEFERRED);

    List<OpsHandoff> open =
        handoffs.list(fx.company(), OpsHandoff.Status.OPEN, PageRequest.of(0, 200)).getContent();
    OpsHandoff handedOver =
        open.stream()
            .filter(
                h -> h.getSourceRef().equals(ref) && h.getPort().equals(HandoffReceiptIssuer.PORT))
            .findFirst()
            .orElseThrow();
    assertThat(handedOver.getAmount()).isEqualByComparingTo("1000.00");
    assertThat(handedOver.getPayload()).contains("COMMISSION");
    OpsHandoff closed =
        as.run(
            "cashier",
            () -> tx.execute(s -> handoffs.close(handedOver.getId(), "OR 0001 issued by hand")));
    assertThat(closed.getStatus()).isEqualTo(OpsHandoff.Status.CLOSED);
    assertThatThrownBy(() -> tx.execute(s -> handoffs.close(handedOver.getId(), "again")))
        .extracting("code")
        .isEqualTo("HANDOFF_CLOSED");
  }

  @Test
  void reapplicationIsRefusedOnlyWhenTheInvoiceHasPayments() {
    OpsInvoice invoice = fx.motorInvoice();
    PaymentReapplier.ReapplyRequest request =
        new PaymentReapplier.ReapplyRequest(
            invoice.getInvoiceNo(), "ADJUSTMENT", "ENR-1", LocalDate.of(2026, 9, 20), "Decrease");
    assertThat(reapplier.reapply(request).excess()).isZero();
    as.run(
        "cashier",
        () ->
            tx.execute(
                s ->
                    ledger.post(
                        new MovementRequest(
                            invoice.getInvoiceNo(),
                            MovementType.APPLIED,
                            "CASHIERING",
                            "APP:" + BookingFixtures.token(),
                            LocalDate.of(2026, 9, 18),
                            Map.of(LedgerComponent.DST, BigDecimal.ONE),
                            null,
                            null))));
    assertThatThrownBy(() -> reapplier.reapply(request))
        .extracting("code")
        .isEqualTo("PAYMENT_REAPPLIER_UNAVAILABLE");
  }

  @Test
  void paymentRequestsGoThroughTheDisbursementQueue() {
    String ref = "RMB-" + BookingFixtures.token();
    DisbursementRequest.Spec spec =
        new DisbursementRequest.Spec(
            DisbursementRequest.Type.REMITTANCE,
            "REMITTANCE",
            ref,
            "INS-MGIC",
            "MGIC",
            "PHP",
            new BigDecimal("25000.00"),
            "Remittance batch " + ref,
            null);
    DisbursementGateway.DisbursementTicket ticket =
        as.run("remit", () -> tx.execute(s -> gateway.send(fx.company(), spec)));
    assertThat(ticket.status()).isEqualTo(DisbursementRequest.Status.SENT);
    assertThat(as.run("remit", () -> tx.execute(s -> gateway.send(fx.company(), spec))).requestNo())
        .isEqualTo(ticket.requestNo());
    Long id = queue.find("REMITTANCE", ref).orElseThrow().getId();

    as.run("disb", () -> tx.execute(s -> queue.acknowledge(id)));
    as.run("disb", () -> tx.execute(s -> queue.assignDv(id, "DV-2026-0001")));
    assertThatThrownBy(() -> tx.execute(s -> queue.acknowledge(id)))
        .extracting("code")
        .isEqualTo("DISBURSEMENT_STATUS");
    as.run("disb", () -> tx.execute(s -> queue.markPaid(id)));
    assertThat(gateway.status("REMITTANCE", ref).orElseThrow().dvNo()).isEqualTo("DV-2026-0001");
    assertThat(events.of(DisbursementStatusChanged.class))
        .anySatisfy(
            e -> {
              assertThat(e.sourceRef()).isEqualTo(ref);
              assertThat(e.status()).isEqualTo(DisbursementRequest.Status.PAID);
            });
    assertThat(
            queue
                .list(
                    fx.company(), List.of(DisbursementRequest.Status.PAID), PageRequest.of(0, 200))
                .getContent())
        .extracting(DisbursementRequest::getSourceRef)
        .contains(ref);

    String other = "RFD-" + BookingFixtures.token();
    Long returned =
        as.run(
            "cashier",
            () ->
                tx.execute(
                    s ->
                        queue
                            .send(
                                fx.company(),
                                new DisbursementRequest.Spec(
                                    DisbursementRequest.Type.REFUND,
                                    "CASHIERING",
                                    other,
                                    BookingFixtures.CLIENT,
                                    null,
                                    "PHP",
                                    BigDecimal.TEN,
                                    "Refund",
                                    null))
                            .getId()));
    as.run("disb", () -> tx.execute(s -> queue.returnToSource(returned, "Bank details missing")));
    assertThat(queue.get(returned).getStatus()).isEqualTo(DisbursementRequest.Status.RETURNED);
    assertThatThrownBy(
            () ->
                tx.execute(
                    s ->
                        queue.send(
                            fx.company(),
                            new DisbursementRequest.Spec(
                                DisbursementRequest.Type.REFUND,
                                "CASHIERING",
                                "ZERO-" + other,
                                "X",
                                null,
                                "PHP",
                                BigDecimal.ZERO,
                                "Nothing",
                                null))))
        .extracting("code")
        .isEqualTo("DISBURSEMENT_AMOUNT");
  }

  @Test
  void extractsAreKeptByFolderAndCollectionItemsAreWrittenOut() {
    String folder = "REMITTANCE/TEST-" + BookingFixtures.token();
    FileDropPort.DroppedFile dropped =
        as.run(
            "remit",
            () ->
                tx.execute(
                    s ->
                        fileDrop.drop(
                            fx.company(),
                            new ExtractFile.Location(folder, "extract.csv"),
                            new DropContent(
                                "text/csv", "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8)),
                            new ExtractFile.Origin("REMITTANCE", "RUN-1"))));
    assertThat(dropped.path()).isEqualTo(folder + "/extract.csv");
    assertThat(extracts.list(fx.company(), folder))
        .singleElement()
        .satisfies(f -> assertThat(f.sizeBytes()).isEqualTo(8));
    byte[] downloaded = tx.execute(s -> extracts.download(dropped.id()).getContent());
    assertThat(downloaded).hasSize(8);
    assertThatThrownBy(
            () ->
                tx.execute(
                    s ->
                        fileDrop.drop(
                            fx.company(),
                            new ExtractFile.Location(folder, "extract.csv"),
                            new DropContent("text/csv", new byte[] {1}),
                            new ExtractFile.Origin("REMITTANCE", "RUN-2"))))
        .isInstanceOf(DuplicateResourceException.class);

    // The manual transport (default until Collections, kept as a fallback) writes a CSV.
    String key = BookingFixtures.token();
    ManualCollectionFeed manual = new ManualCollectionFeed(flowIn, extracts);
    String runNo =
        as.run(
            "commrec",
            () ->
                manual.send(
                    fx.company(),
                    "COLLECTION_DP_RETURNED",
                    List.of(
                        new FeedItem(
                            key,
                            Map.of("invoice", "BI-1, returned", "reason", "Rejected \"late\"")))));
    assertThat(extracts.list(fx.company(), "COLLECTION/COLLECTION_DP_RETURNED"))
        .anySatisfy(f -> assertThat(f.fileName()).isEqualTo(runNo + ".csv"));
  }

  @Test
  void bookRatesAreTwoDecimalRatesOfTheCurrencyMaster() {
    LocalDate day = LocalDate.of(2026, 9, 15);
    assertThat(rates.rate(fx.company(), "PHP", day)).isEqualByComparingTo(BigDecimal.ONE);
    BigDecimal usd = rates.rate(fx.company(), "USD", day);
    assertThat(usd.scale()).isEqualTo(2);
    assertThat(usd).isGreaterThan(BigDecimal.TEN);
  }
}
