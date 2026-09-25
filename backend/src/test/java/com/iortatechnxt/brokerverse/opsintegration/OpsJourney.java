package com.iortatechnxt.brokerverse.opsintegration;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.OpenItemRole;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentChannel;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentIntake;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeResult;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeTarget;
import com.iortatechnxt.brokerverse.opsledger.OpsLedgerFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The Operations journey of an invoice, driven through the real services as the demo users, with
 * the ledger, journal and open-item checks every step of {@link OperationsEndToEndIT} repeats.
 */
@Component
public class OpsJourney {

  /** Value date of the journey's payments: well past the check holding period (RMTID.017). */
  public static final LocalDate PAID_ON = LocalDate.of(2026, 9, 16);

  private final OpsLedgerFixtures ledgerFx;
  private final InvoiceLedgerQueryService ledger;
  private final BookingQueryService bookings;
  private final PaymentIntakeService intake;
  private final TestData data;
  private final AsUser as;
  private final TransactionTemplate tx;
  private final JdbcTemplate jdbc;

  OpsJourney(
      OpsLedgerFixtures ledgerFx,
      InvoiceLedgerQueryService ledger,
      BookingQueryService bookings,
      PaymentIntakeService intake,
      TestData data,
      AsUser as,
      TransactionTemplate tx,
      JdbcTemplate jdbc) {
    this.ledgerFx = ledgerFx;
    this.ledger = ledger;
    this.bookings = bookings;
    this.intake = intake;
    this.data = data;
    this.as = as;
    this.tx = tx;
    this.jdbc = jdbc;
  }

  /** The demo company. */
  public Long company() {
    return ledgerFx.company();
  }

  /** Books a motor account paid via BDOI (proc) and returns its booked invoice. */
  public BookedInvoice bookMotor() {
    return ledgerFx.bookMotor();
  }

  /** Books a direct payment motor account (proc). */
  public BookedInvoice bookDirectPayment() {
    return ledgerFx.bookDirectPayment();
  }

  /** The ledger invoice with its components and shares. */
  public OpsInvoice invoice(String invoiceNo) {
    return tx.execute(
        s -> {
          OpsInvoice i = ledger.require(invoiceNo);
          i.loadCollections();
          return i;
        });
  }

  /** The balance of one component. */
  public BigDecimal balance(String invoiceNo, LedgerComponent component) {
    return invoice(invoiceNo).component(component).getBalance();
  }

  /** The movements of an invoice of one type. */
  public List<OpsInvoiceMovement> movements(String invoiceNo, MovementType type) {
    return ledger.movements(invoiceNo).stream().filter(m -> m.getMovementType() == type).toList();
  }

  /** The sum of the movements of one type and component. */
  public BigDecimal moved(String invoiceNo, MovementType type, LedgerComponent component) {
    return movements(invoiceNo, type).stream()
        .filter(m -> m.getComponent() == component)
        .map(OpsInvoiceMovement::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Receives an over-the-counter cash payment as the cashier at Head Office. */
  public IntakeResult pay(String reference, BigDecimal amount) {
    return as.run(
        "cashier",
        () ->
            intake.receive(
                new IntakeTarget(company(), data.branch("HO").getId(), "OTC", ReceiptSource.OTC),
                new PaymentIntake(
                    PaymentChannel.OTC,
                    null,
                    "E2E:" + BookingFixtures.token(),
                    null,
                    reference,
                    List.of(),
                    new PaymentIntake.Payor(null, "Journey Payor " + reference),
                    null,
                    new PaymentIntake.Money(amount, "PHP", PAID_ON),
                    PaymentIntake.Tender.of(PaymentMode.CASH))));
  }

  /**
   * The accounting events posted for a source reference, each with its balanced journal.
   *
   * @param sourceRef source reference of the events
   * @return event types, in posting order
   */
  public List<String> postedEvents(String sourceRef) {
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "select e.event_type, e.status, b.total_debit, b.total_credit from acc_event_log e"
                + " left join jnl_batch b on b.batch_no = e.batch_no"
                + " where e.source_reference = ? order by e.id",
            sourceRef);
    assertThat(rows)
        .as("journal of %s", sourceRef)
        .allSatisfy(
            r -> {
              assertThat(r.get("status")).isEqualTo("POSTED");
              assertThat((BigDecimal) r.get("total_debit"))
                  .isPositive()
                  .isEqualByComparingTo((BigDecimal) r.get("total_credit"));
            });
    return rows.stream().map(r -> (String) r.get("event_type")).toList();
  }

  /**
   * Checks that every journal batch the ledger movements of an invoice point to is posted and
   * balanced, and returns how many there are.
   *
   * @param invoiceNo invoice
   * @return number of journal batches
   */
  public int balancedJournals(String invoiceNo) {
    List<String> batchNos =
        ledger.movements(invoiceNo).stream()
            .map(OpsInvoiceMovement::getJournalBatchNo)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    for (String batchNo : batchNos) {
      List<Map<String, Object>> batch =
          jdbc.queryForList(
              "select total_debit, total_credit from jnl_batch where batch_no = ?", batchNo);
      assertThat(batch)
          .as(
              "journal %s of %s",
              batchNo,
              ledger.movements(invoiceNo).stream()
                  .filter(m -> batchNo.equals(m.getJournalBatchNo()))
                  .map(m -> m.getSourceModule() + " " + m.getSourceRef())
                  .toList())
          .singleElement()
          .satisfies(
              b ->
                  assertThat((BigDecimal) b.get("total_debit"))
                      .isEqualByComparingTo((BigDecimal) b.get("total_credit")));
    }
    return batchNos.size();
  }

  /**
   * The booking open items of an invoice by role, with their outstanding amount.
   *
   * @param invoiceNo invoice
   * @return outstanding per role
   */
  public Map<OpenItemRole, BigDecimal> openItems(String invoiceNo) {
    BookedInvoice booked = bookings.byNo(invoiceNo);
    return bookings.openItems(booked).stream()
        .collect(
            Collectors.toMap(
                BookingQueryService.InvoiceItem::role,
                i -> i.item().outstanding(),
                BigDecimal::add));
  }

  /** Runs as a demo user. */
  public <T> T as(String user, java.util.function.Supplier<T> work) {
    return as.run(user, work);
  }

  /** Runs in a transaction as a demo user. */
  public <T> T inTx(String user, java.util.function.Supplier<T> work) {
    return as.run(user, () -> tx.execute(s -> work.get()));
  }
}
