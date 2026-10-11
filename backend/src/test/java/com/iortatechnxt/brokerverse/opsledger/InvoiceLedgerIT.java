package com.iortatechnxt.brokerverse.opsledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.EndorsementType;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.service.EndorsementPosting;
import com.iortatechnxt.brokerverse.booking.service.EndorsementPostingService;
import com.iortatechnxt.brokerverse.booking.service.EndorsementResult;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import com.iortatechnxt.brokerverse.opsledger.domain.FeedSource;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.RunStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.opsledger.domain.InvoiceFlag;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.Invoice360Service;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceFeedReplayService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService.FlagChange;
import com.iortatechnxt.brokerverse.opsledger.service.LedgerSearch;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceFlagChanged;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceLocked;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceMovementPosted;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.OpsInvoiceBooked;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** The Operations invoice ledger: feed from booking, movements, flags, locks, 360 and replay. */
@IntegrationTest
class InvoiceLedgerIT {

  private static final LocalDate PAID_ON = LocalDate.of(2026, 9, 18);
  private static final String CASHIERING = "CASHIERING";
  private static final String REMITTANCE = "REMITTANCE";
  private static final String ADJUSTMENT = "ADJUSTMENT";

  @Autowired private OpsLedgerFixtures fx;
  @Autowired private InvoiceLedgerQueryService queries;
  @Autowired private InvoiceLedgerService ledger;
  @Autowired private Invoice360Service views;
  @Autowired private InvoiceFeedReplayService replay;
  @Autowired private EndorsementPostingService endorsements;
  @Autowired private CapturedLedgerEvents events;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TransactionTemplate tx;

  private List<OpsInvoiceMovement> pay(
      OpsInvoice invoice, String ref, Map<LedgerComponent, BigDecimal> amounts) {
    return as.run(
        "proc",
        () ->
            tx.execute(
                s ->
                    ledger.post(
                        new MovementRequest(
                            invoice.getInvoiceNo(),
                            MovementType.APPLIED,
                            CASHIERING,
                            ref,
                            PAID_ON,
                            amounts,
                            new MovementRequest.DocumentRefs("AR-TEST-" + ref, null, null, null),
                            "Test payment"))));
  }

  private static String ref() {
    return "APP:" + BookingFixtures.token();
  }

  @Test
  void aBookedInvoiceIsCopiedWithItsComponentsAfterCommit() {
    BookedInvoice booked = fx.bookMotor();
    OpsInvoice invoice = fx.ledgerOf(booked);

    assertThat(invoice.getArn()).isEqualTo(booked.getArn());
    assertThat(invoice.getKind()).isEqualTo(InvoiceKind.BOOKING);
    assertThat(invoice.getFeedSource()).isEqualTo(FeedSource.EVENT);
    assertThat(invoice.getClientCode()).isEqualTo(BookingFixtures.CLIENT);
    assertThat(invoice.getAssuredName()).isEqualTo(booked.getFacts().clientName());
    assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    assertThat(invoice.getRemittanceStatus()).isEqualTo(RemittanceStatus.WITH_OUTSTANDING_BALANCE);
    assertThat(invoice.getShares()).singleElement().satisfies(s -> assertThat(s.lead()).isTrue());
    assertThat(invoice.component(LedgerComponent.BASIC).getBooked())
        .isEqualByComparingTo(booked.getPremium().basic());
    assertThat(invoice.component(LedgerComponent.DTIP).getBalance())
        .isEqualByComparingTo(booked.getPremium().total());
    assertThat(invoice.component(LedgerComponent.COMMISSION).getBooked())
        .isEqualByComparingTo(booked.getCommission().commission());
    assertThat(invoice.component(LedgerComponent.WTAX).getBooked())
        .isEqualByComparingTo(booked.getCommission().wtaxAmount());
    assertThat(invoice.premiumBalance()).isEqualByComparingTo(booked.getPremium().total());
    assertThat(queries.movements(invoice.getInvoiceNo()))
        .isNotEmpty()
        .allSatisfy(m -> assertThat(m.getMovementType()).isEqualTo(MovementType.BOOKED))
        .allSatisfy(m -> assertThat(m.getSourceRef()).isEqualTo(invoice.getInvoiceNo()));
    assertThat(events.of(OpsInvoiceBooked.class))
        .anySatisfy(e -> assertThat(e.invoiceNo()).isEqualTo(invoice.getInvoiceNo()));
    assertThat(queries.forArn(booked.getArn()))
        .extracting(OpsInvoice::getInvoiceNo)
        .containsExactly(invoice.getInvoiceNo());
  }

  @Test
  void paymentsMoveBalancesAndStatusesOnceAndAreHistorised() {
    OpsInvoice invoice = fx.motorInvoice();
    BigDecimal dst = invoice.component(LedgerComponent.DST).getBooked();
    String first = ref();
    List<OpsInvoiceMovement> moved = pay(invoice, first, Map.of(LedgerComponent.DST, dst));
    assertThat(moved).singleElement().satisfies(m -> assertThat(m.getArNo()).startsWith("AR-TEST"));
    assertThat(pay(invoice, first, Map.of(LedgerComponent.DST, dst)))
        .extracting(OpsInvoiceMovement::getId)
        .containsExactly(moved.get(0).getId());

    OpsInvoice partial = queries.require(invoice.getInvoiceNo());
    assertThat(partial.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
    assertThat(partial.component(LedgerComponent.DST).getBalance()).isZero();

    Map<LedgerComponent, BigDecimal> rest = new EnumMap<>(LedgerComponent.class);
    partial.getComponents().stream()
        .filter(c -> c.getComponent().isPremiumReceivable() && c.getBalance().signum() > 0)
        .forEach(c -> rest.put(c.getComponent(), c.getBalance()));
    pay(invoice, ref(), rest);

    OpsInvoice paid = queries.require(invoice.getInvoiceNo());
    assertThat(paid.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(paid.getRemittanceStatus()).isEqualTo(RemittanceStatus.UNPROCESSED);
    assertThat(paid.premiumBalance()).isZero();
    assertThat(queries.history(invoice.getInvoiceNo()))
        .extracting(c -> c.getField() + ":" + c.getToValue())
        .contains(
            "PAYMENT_STATUS:PARTIALLY_PAID",
            "PAYMENT_STATUS:PAID",
            "REMITTANCE_STATUS:UNPROCESSED");
    assertThat(events.of(InvoiceMovementPosted.class))
        .anySatisfy(e -> assertThat(e.sourceRef()).isEqualTo(first));
    assertThat(
            queries
                .search(
                    new LedgerSearch(
                        fx.company(),
                        invoice.getInvoiceNo(),
                        null,
                        null,
                        PaymentStatus.PAID,
                        RemittanceStatus.UNPROCESSED,
                        null,
                        false,
                        false,
                        null,
                        null),
                    PageRequest.of(0, 5))
                .getContent())
        .extracting(OpsInvoice::getInvoiceNo)
        .containsExactly(invoice.getInvoiceNo());
  }

  @Test
  void aLockBlocksOtherModulesExceptPaymentsAndFlagsAreLogged() {
    OpsInvoice invoice = fx.motorInvoice();
    String no = invoice.getInvoiceNo();
    as.run("remit", () -> tx.execute(s -> ledger.lock(no, REMITTANCE, "In remittance batch")));
    assertThat(queries.require(no).getLockOwner()).isEqualTo(REMITTANCE);
    assertThat(events.of(InvoiceLocked.class))
        .anySatisfy(e -> assertThat(e.invoiceNo()).isEqualTo(no));

    assertThatThrownBy(
            () ->
                as.run(
                    "adjust",
                    () ->
                        tx.execute(
                            s ->
                                ledger.post(
                                    new MovementRequest(
                                        no,
                                        MovementType.ADJUSTED,
                                        ADJUSTMENT,
                                        "ADJ:" + BookingFixtures.token(),
                                        PAID_ON,
                                        Map.of(LedgerComponent.BASIC, BigDecimal.TEN.negate()),
                                        null,
                                        null)))))
        .extracting("code")
        .isEqualTo("INVOICE_LOCKED");
    assertThatThrownBy(() -> ledger.requireUnlocked(no, ADJUSTMENT))
        .extracting("code")
        .isEqualTo("INVOICE_LOCKED");
    assertThatThrownBy(
            () ->
                tx.execute(
                    s ->
                        ledger.setFlag(
                            new FlagChange(
                                no, InvoiceFlag.PENDING_NEG_ADJ, true, ADJUSTMENT, "x"))))
        .extracting("code")
        .isEqualTo("INVOICE_LOCKED");
    pay(invoice, ref(), Map.of(LedgerComponent.DST, BigDecimal.ONE));
    assertThatThrownBy(() -> tx.execute(s -> ledger.unlock(no, ADJUSTMENT, null)))
        .extracting("code")
        .isEqualTo("INVOICE_LOCKED");

    as.run(
        "remit",
        () ->
            tx.execute(
                s -> {
                  ledger.setFlag(
                      new FlagChange(no, InvoiceFlag.HOLD, true, REMITTANCE, "Client dispute"));
                  ledger.setRemittanceStatus(
                      no, RemittanceStatus.REVIEW_IN_PROCESS, REMITTANCE, "RMB-1");
                  return ledger.unlock(no, REMITTANCE, "Batch returned");
                }));
    OpsInvoice after = queries.require(no);
    assertThat(after.getLockOwner()).isNull();
    assertThat(after.isHoldFlag()).isTrue();
    assertThat(after.getRemittanceStatus()).isEqualTo(RemittanceStatus.REVIEW_IN_PROCESS);
    assertThat(queries.history(no))
        .extracting(c -> c.getField() + ":" + c.getToValue())
        .contains(
            "LOCK:REMITTANCE", "HOLD:true", "REMITTANCE_STATUS:REVIEW_IN_PROCESS", "LOCK:null");
    assertThat(events.of(InvoiceFlagChanged.class))
        .anySatisfy(e -> assertThat(e.invoiceNo()).isEqualTo(no));
    assertThat(
            queries
                .search(
                    new LedgerSearch(
                        fx.company(),
                        no,
                        null,
                        null,
                        null,
                        null,
                        InvoiceFlag.HOLD,
                        null,
                        null,
                        null,
                        null),
                    PageRequest.of(0, 5))
                .getTotalElements())
        .isEqualTo(1);
  }

  @Test
  void anEndorsementIsANewInvoiceCountedAgainstTheOriginal() {
    BookedInvoice original = fx.bookMotor();
    EndorsementResult result =
        as.run(
            "proc",
            () ->
                endorsements.post(
                    new EndorsementPosting(
                        original.getArn(),
                        EndorsementType.POSITIVE,
                        null,
                        LocalDate.of(2027, 4, 1),
                        PeriodBasis.PRO_RATA,
                        new BigDecimal("100000"),
                        null,
                        null,
                        null,
                        "Sum insured increase",
                        null,
                        LocalDate.of(2026, 9, 20),
                        "OPS:" + BookingFixtures.token())));
    OpsInvoice endorsement = queries.require(result.invoiceNo());
    assertThat(endorsement.getKind()).isEqualTo(InvoiceKind.ENDORSEMENT_PLUS);
    assertThat(endorsement.getParentInvoiceNo()).isEqualTo(original.getInvoiceNo());
    // One invoice family: the endorsement carries the root of its parent (DIS 3.27.2).
    assertThat(queries.require(original.getInvoiceNo()).getRootInvoiceNo())
        .isEqualTo(original.getInvoiceNo());
    assertThat(endorsement.getRootInvoiceNo()).isEqualTo(original.getInvoiceNo());
    assertThat(queries.family(result.invoiceNo()))
        .extracting(OpsInvoice::getInvoiceNo)
        .containsExactly(original.getInvoiceNo(), result.invoiceNo());
    assertThat(queries.family("NO-SUCH-INVOICE")).isEmpty();
    var total = queries.adjustmentTotal(original.getInvoiceNo()).orElseThrow();
    assertThat(total.getAdjustmentCount()).isEqualTo(1);
    assertThat(total.getAdjustedPremium()).isEqualByComparingTo(endorsement.getGrossPremium());
    assertThat(total.isOverAdjusted()).isFalse();

    var view = views.view(result.invoiceNo());
    assertThat(view.adjustments()).isNotNull();
    assertThat(view.booking().bookedInvoiceId()).isNotNull();
    assertThat(view.movements()).isNotEmpty();
  }

  @Test
  void aDirectPaymentInvoiceHasNothingToCollectOrRemit() {
    OpsInvoice invoice = fx.ledgerOf(fx.bookDirectPayment());
    assertThat(invoice.isDpFlag()).isTrue();
    assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.NOT_APPLICABLE);
    assertThat(invoice.getRemittanceStatus()).isEqualTo(RemittanceStatus.NOT_APPLICABLE);
    assertThat(invoice.component(LedgerComponent.COMMISSION).getBalance()).isPositive();
  }

  @Test
  void missingInvoicesAreReplayedFromBookingAsALoggedRun() {
    BookedInvoice booked = fx.bookMotor();
    String no = booked.getInvoiceNo();
    tx.executeWithoutResult(
        s -> {
          Long id =
              jdbc.queryForObject(
                  "select id from ops_invoice where invoice_no = ?", Long.class, no);
          jdbc.update("delete from ops_invoice_movement where invoice_id = ?", id);
          jdbc.update("delete from ops_invoice_component where invoice_id = ?", id);
          jdbc.update("delete from ops_invoice_share where invoice_id = ?", id);
          jdbc.update("delete from ops_invoice_status_change where invoice_id = ?", id);
          jdbc.update("delete from ops_invoice where id = ?", id);
        });
    assertThat(queries.find(no)).isEmpty();

    FlowInRun run = as.run("admin", () -> replay.replayAccount(booked.getArn()));
    assertThat(run.getStatus()).isEqualTo(RunStatus.SUCCEEDED);
    assertThat(run.getOkCount()).isEqualTo(1);
    OpsInvoice restored = queries.require(no);
    assertThat(restored.getFeedSource()).isEqualTo(FeedSource.REPLAY);
    assertThat(restored.premiumBalance()).isEqualByComparingTo(booked.getPremium().total());

    FlowInRun again = replay.replayInvoice(no);
    assertThat(again.getReadCount()).isZero();
    assertThat(again.getStatus()).isEqualTo(RunStatus.SUCCEEDED);
  }

  @Test
  void aMovementNeedsAnAmountAndBookedIsReserved() {
    OpsInvoice invoice = fx.motorInvoice();
    assertThatThrownBy(
            () ->
                tx.execute(
                    s ->
                        ledger.post(
                            new MovementRequest(
                                invoice.getInvoiceNo(),
                                MovementType.APPLIED,
                                CASHIERING,
                                ref(),
                                PAID_ON,
                                Map.of(LedgerComponent.DST, BigDecimal.ZERO),
                                null,
                                null))))
        .extracting("code")
        .isEqualTo("MOVEMENT_WITHOUT_AMOUNT");
    assertThatThrownBy(
            () ->
                tx.execute(
                    s ->
                        ledger.post(
                            new MovementRequest(
                                invoice.getInvoiceNo(),
                                MovementType.BOOKED,
                                CASHIERING,
                                ref(),
                                PAID_ON,
                                Map.of(LedgerComponent.DST, BigDecimal.ONE),
                                null,
                                null))))
        .extracting("code")
        .isEqualTo("MOVEMENT_TYPE_RESERVED");
    assertThatThrownBy(() -> queries.require("NO-SUCH-INVOICE"))
        .hasMessageContaining("NO-SUCH-INVOICE");
  }
}
