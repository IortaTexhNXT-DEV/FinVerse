package com.iortatechnxt.brokerverse.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.domain.InsurerShare;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.domain.OpenItemRole;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponent;
import com.iortatechnxt.brokerverse.booking.domain.QueueSource;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.SiKind;
import com.iortatechnxt.brokerverse.booking.service.BookingClientRecords;
import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import com.iortatechnxt.brokerverse.booking.service.BookingPreviewService;
import com.iortatechnxt.brokerverse.booking.service.BookingPreviewService.BookingPreview;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService.InvoiceItem;
import com.iortatechnxt.brokerverse.booking.service.BookingQueueService;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.booking.service.BookingSettings;
import com.iortatechnxt.brokerverse.booking.service.InvoiceBooked;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceRegister;
import com.iortatechnxt.brokerverse.subledger.domain.ItemDirection;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Booking: journal, open items, service invoice, event, direct payment, idempotency, rollback. */
@IntegrationTest
class BookingServiceIT {

  @Autowired private BookingFixtures fx;
  @Autowired private BookingService booking;
  @Autowired private BookingPreviewService preview;
  @Autowired private BookingQueryService queries;
  @Autowired private BookingQueueService queue;
  @Autowired private ServiceInvoiceRegister register;
  @Autowired private BookingClientRecords clientRecords;
  @Autowired private AccountQueryService accounts;
  @Autowired private CapturedInvoiceEvents events;
  @Autowired private SystemParameterService parameters;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private BookedInvoice book(String arn, BookingOptions options) {
    return as.run("proc", () -> booking.book(arn, options, BookingSource.INDIVIDUAL));
  }

  private List<Map<String, Object>> lines(String batchNo) {
    return jdbc.queryForList(
        "select a.code, l.side, l.amount, l.party_code from jnl_line l"
            + " join jnl_batch b on b.id = l.batch_id join coa_account a on a.id = l.account_id"
            + " where b.batch_no = ? order by l.line_no",
        batchNo);
  }

  private static BigDecimal amount(List<Map<String, Object>> lines, String code, String side) {
    return lines.stream()
        .filter(l -> code.equals(l.get("code")) && side.equals(l.get("side")))
        .map(l -> (BigDecimal) l.get("amount"))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  @Test
  void bookingPostsBalancedJournalOpenItemsServiceInvoiceAndEvent() {
    Account account = fx.motor();
    BookingPreview shown =
        as.run(
            "proc",
            () ->
                preview.preview(
                    account.getArn(), BookingOptions.of(BookingFixtures.BOOKED_ON, null)));
    assertThat(shown.invoices()).hasSize(1);
    assertThat(shown.journal()).isNotEmpty();

    BookedInvoice invoice =
        book(account.getArn(), BookingOptions.of(BookingFixtures.BOOKED_ON, null));

    assertThat(invoice.getInvoiceNo()).startsWith("BI-HO-2026-");
    BookedInvoice loaded = queries.get(invoice.getId());
    BigDecimal gross = account.getPremium().grossPremium();
    assertThat(loaded.getPremium().total()).isEqualByComparingTo(gross);
    assertThat(loaded.getPremium().basic()).isEqualByComparingTo(account.getPremium().netPremium());
    assertThat(loaded.getFacts().costCenter()).isEqualTo("NB-CBG-M");
    assertThat(loaded.getFlags().incentiveEligible()).isTrue();
    assertThat(loaded.getCommission().wtaxRate()).isEqualByComparingTo("10");

    List<Map<String, Object>> journal = lines(loaded.getJournalBatches().get(0));
    BigDecimal debit =
        amount(journal, "1210.01", "DEBIT")
            .add(amount(journal, "1210.02", "DEBIT"))
            .add(amount(journal, "1210.03", "DEBIT"))
            .add(amount(journal, "1210.04", "DEBIT"))
            .add(amount(journal, "1220", "DEBIT"));
    BigDecimal credit =
        amount(journal, "2210", "CREDIT")
            .add(amount(journal, "2220", "CREDIT"))
            .add(amount(journal, "2221", "CREDIT"));
    assertThat(debit).isEqualByComparingTo(credit);
    assertThat(amount(journal, "2210", "CREDIT")).isEqualByComparingTo(gross);
    assertThat(amount(journal, "2220", "CREDIT"))
        .isEqualByComparingTo(account.getPremium().commission());
    assertThat(journal)
        .filteredOn(l -> "1210.01".equals(l.get("code")))
        .extracting(l -> l.get("party_code"))
        .containsOnly(BookingFixtures.CLIENT);
    assertThat(journal)
        .filteredOn(l -> "2210".equals(l.get("code")))
        .extracting(l -> l.get("party_code"))
        .containsOnly("INS-MGIC");

    List<InvoiceItem> items = queries.openItems(loaded);
    assertThat(items)
        .extracting(InvoiceItem::role)
        .containsExactly(
            OpenItemRole.CLIENT_PREMIUM,
            OpenItemRole.INSURER_DTIP,
            OpenItemRole.INSURER_COMMISSION);
    InvoiceItem pr = items.get(0);
    assertThat(pr.item().getDirection()).isEqualTo(ItemDirection.DEBIT);
    assertThat(pr.item().getAmount()).isEqualByComparingTo(gross);
    assertThat(items.get(1).item().getDirection()).isEqualTo(ItemDirection.CREDIT);
    assertThat(items.get(2).item().getAmount())
        .isEqualByComparingTo(loaded.getCommission().receivable());

    List<ServiceInvoice> sis = register.forInvoice(loaded.getInvoiceNo());
    assertThat(sis).hasSize(1);
    ServiceInvoice si = sis.get(0);
    assertThat(si.getSiNo()).startsWith("SI-HO-2026-");
    assertThat(si.getKind()).isEqualTo(SiKind.INVOICE);
    assertThat(si.getNetAmount())
        .isEqualByComparingTo(
            loaded.getCommission().receivable().subtract(loaded.getCommission().wtaxAmount()));
    assertThat(loaded.getServiceInvoiceNo()).isEqualTo(si.getSiNo());

    Account booked = accounts.requireByArn(account.getArn());
    assertThat(booked.getStatus()).isEqualTo(AccountStatus.BOOKED);
    assertThat(booked.getLifecycle().getBookingRef()).isEqualTo(loaded.getInvoiceNo());
    assertThat(booked.getLifecycle().isIncentiveFlag()).isTrue();

    InvoiceBooked event = events.forInvoice(loaded.getInvoiceNo()).orElseThrow();
    assertThat(event.kind()).isEqualTo(InvoiceKind.BOOKING);
    assertThat(event.arn()).isEqualTo(account.getArn());
    assertThat(event.endorsementNo()).isNull();
    assertThat(event.clientCode()).isEqualTo(BookingFixtures.CLIENT);
    assertThat(event.shares())
        .containsExactly(new InvoiceBooked.Share("INS-MGIC", new BigDecimal("100.0000")));
    assertThat(event.currency()).isEqualTo("PHP");
    assertThat(event.bookingDate()).isEqualTo(BookingFixtures.BOOKED_ON);
    assertThat(event.inceptionDate()).isEqualTo(BookingFixtures.FROM);
    assertThat(event.expiryDate()).isEqualTo(BookingFixtures.TO);
    assertThat(event.riskCode()).isEqualTo("MTR10");
    assertThat(event.segment()).isEqualTo("CBG");
    assertThat(event.aoUsername()).isEqualTo("ao");
    assertThat(event.salesUnit()).isNotBlank();
    assertThat(event.costCenter()).isEqualTo("NB-CBG-M");
    assertThat(event.components()).containsKeys(PremiumComponent.values());
    assertThat(event.grossPremium()).isEqualByComparingTo(gross);
    assertThat(event.commission()).isEqualByComparingTo(account.getPremium().commission());
    assertThat(event.vatOnCommission())
        .isEqualByComparingTo(account.getPremium().vatOnCommission());
    assertThat(event.wtaxRate()).isEqualByComparingTo("10");
    assertThat(event.directPayment()).isFalse();
    assertThat(event.cwt2Percent()).isFalse();
    assertThat(event.incentiveEligible()).isTrue();
    assertThat(queries.invoice(loaded.getInvoiceNo())).isEqualTo(event);
    assertThat(queries.invoicesForArn(account.getArn())).containsExactly(event);
    assertThat(clientRecords.recordsOf(account.getClientId()))
        .anyMatch(r -> r.reference().equals(loaded.getInvoiceNo()));
  }

  @Test
  void directPaymentBooksOnlyTheCommissionReceivable() {
    Account account = fx.directPaymentMotor();
    BookedInvoice invoice =
        book(account.getArn(), new BookingOptions(BookingFixtures.BOOKED_ON, "MKT", true, null));
    BookedInvoice loaded = queries.get(invoice.getId());
    assertThat(loaded.getFlags().directPayment()).isTrue();
    assertThat(loaded.getFlags().cwt2Percent()).isTrue();
    assertThat(loaded.getFacts().costCenter()).isEqualTo("MKT");
    List<Map<String, Object>> journal = lines(loaded.getJournalBatches().get(0));
    assertThat(journal).extracting(l -> l.get("code")).containsOnly("1220", "2220", "2221");
    assertThat(queries.openItems(loaded))
        .extracting(InvoiceItem::role)
        .containsExactly(OpenItemRole.INSURER_COMMISSION);
    assertThat(events.forInvoice(loaded.getInvoiceNo()).orElseThrow().directPayment()).isTrue();
  }

  @Test
  void anAccountIsBookedOnlyOnce() {
    Account account = fx.motor();
    book(account.getArn(), BookingOptions.of(BookingFixtures.BOOKED_ON, null));
    assertThatThrownBy(
            () -> book(account.getArn(), BookingOptions.of(BookingFixtures.BOOKED_ON, null)))
        .extracting("code")
        .isEqualTo("DUPLICATE_BOOKING");
    assertThat(
            as.run(
                "proc",
                () -> queue.enqueue(fx.company(), List.of(account.getArn()), QueueSource.MANUAL)))
        .singleElement()
        .satisfies(r -> assertThat(r.queued()).isFalse());
    assertThat(queries.invoicesForArn(account.getArn())).hasSize(1);
  }

  @Test
  void aFailedPostingRollsTheWholeBookingBack() {
    Account account = fx.motor();
    String before =
        jdbc.queryForObject(
            "select coalesce(max(invoice_no), '') from bkg_invoice where invoice_no like 'BI-HO-2026-%'",
            String.class);
    assertThatThrownBy(
            () -> book(account.getArn(), BookingOptions.of(LocalDate.of(2025, 12, 15), null)))
        .isInstanceOf(RuntimeException.class);
    assertThat(queries.schedule(account.getArn())).isEmpty();
    assertThat(accounts.requireByArn(account.getArn()).getStatus())
        .isEqualTo(AccountStatus.POLICY_ISSUED);
    assertThat(events.forArn(account.getArn())).isEmpty();

    BookedInvoice invoice =
        book(account.getArn(), BookingOptions.of(BookingFixtures.BOOKED_ON, null));
    long previous =
        before.isEmpty() ? 0 : Long.parseLong(before.substring(before.lastIndexOf('-') + 1));
    assertThat(invoice.getInvoiceNo()).isEqualTo(String.format("BI-HO-2026-%06d", previous + 1));
  }

  @Test
  void invalidChoicesAreRefused() {
    Account account = fx.motor();
    assertThatThrownBy(
            () ->
                book(
                    account.getArn(),
                    new BookingOptions(BookingFixtures.BOOKED_ON, "NOPE", null, null)))
        .extracting("code")
        .isEqualTo("INVALID_DIMENSION");
    assertThatThrownBy(
            () ->
                book(
                    account.getArn(),
                    new BookingOptions(
                        BookingFixtures.BOOKED_ON,
                        null,
                        null,
                        List.of(new InsurerShare("INS-MGIC", new BigDecimal("70"))))))
        .extracting("code")
        .isEqualTo("INSURER_SHARES_INVALID");
    assertThatThrownBy(
            () -> book(account.getArn(), BookingOptions.of(LocalDate.now().plusDays(3), null)))
        .extracting("code")
        .isEqualTo("BOOKING_DATE_FUTURE");
    assertThatThrownBy(() -> booking.requireBookable("ARN-1900-000000"))
        .hasMessageContaining("ARN-1900-000000");
  }

  @Test
  void coInsuranceSplitsJournalsOpenItemsAndServiceInvoicesByShare() {
    Account account = fx.motor();
    BookedInvoice invoice =
        book(
            account.getArn(),
            new BookingOptions(
                BookingFixtures.BOOKED_ON,
                null,
                null,
                List.of(
                    new InsurerShare("INS-MGIC", new BigDecimal("60")),
                    new InsurerShare("INS-LAC", new BigDecimal("40")))));
    BookedInvoice loaded = queries.get(invoice.getId());
    assertThat(loaded.getJournalBatches()).hasSize(2);
    List<InvoiceItem> items = queries.openItems(loaded);
    BigDecimal dtip =
        items.stream()
            .filter(i -> i.role() == OpenItemRole.INSURER_DTIP)
            .map(i -> i.item().getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(dtip).isEqualByComparingTo(loaded.getPremium().total());
    List<ServiceInvoice> sis = register.forInvoice(loaded.getInvoiceNo());
    assertThat(sis)
        .extracting(ServiceInvoice::getRecipientCode)
        .containsExactly("INS-MGIC", "INS-LAC");
    assertThat(
            sis.stream()
                .map(ServiceInvoice::getCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo(loaded.getCommission().commission());
  }

  @Test
  void commissionIsRealizedAtBookingWhenTheParameterSaysSo() {
    Account account = fx.motor();
    parameters.update(BookingSettings.COMMISSION_REALIZATION, BookingSettings.ON_BOOKING);
    try {
      BookedInvoice invoice =
          book(account.getArn(), BookingOptions.of(BookingFixtures.BOOKED_ON, null));
      List<Map<String, Object>> journal =
          lines(queries.get(invoice.getId()).getJournalBatches().get(0));
      assertThat(amount(journal, "4101", "CREDIT"))
          .isEqualByComparingTo(account.getPremium().commission());
      assertThat(amount(journal, "2504", "CREDIT"))
          .isEqualByComparingTo(account.getPremium().vatOnCommission());
      assertThat(amount(journal, "2220", "CREDIT")).isZero();
    } finally {
      parameters.update(BookingSettings.COMMISSION_REALIZATION, "ON_COLLECTION");
    }
  }
}
