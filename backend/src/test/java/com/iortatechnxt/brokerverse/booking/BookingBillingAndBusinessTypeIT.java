package com.iortatechnxt.brokerverse.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.domain.BusinessType;
import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.booking.service.BookingSettings;
import com.iortatechnxt.brokerverse.booking.service.BookingUploadHandler;
import com.iortatechnxt.brokerverse.booking.service.InsurerBillingNumbers;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Shared work item BT0 in booking (the invoice takes the account's business type, BRNB.097,
 * BRID-022.01) and the insurer billing number (BRID-020): required for the lines of {@code
 * BOOKING_BILLING_NO_LINES}, unique per company and insurer, audited and notified.
 */
@IntegrationTest
class BookingBillingAndBusinessTypeIT {

  private static final String DEFAULT_LINES = "HMO,GLI,GPA";

  @Autowired private BookingFixtures fx;
  @Autowired private BookingService booking;
  @Autowired private BookingUploadHandler upload;
  @Autowired private CapturedInvoiceEvents events;
  @Autowired private SystemParameterService parameters;
  @Autowired private ReportService reports;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private BookedInvoice book(String arn, String billingNo) {
    return as.run(
        "proc",
        () ->
            booking.book(
                arn,
                BookingOptions.of(BookingFixtures.BOOKED_ON, null).withBillingNo(billingNo),
                BookingSource.INDIVIDUAL));
  }

  @Test
  void aRenewalAccountBooksAsRenewalAndNewBusinessStaysNewBusiness() {
    Account renewal = fx.renewalMotor("ARN-2025-" + BookingFixtures.token());
    Account newBusiness = fx.motor();

    BookedInvoice renewed = book(renewal.getArn(), null);
    BookedInvoice fresh = book(newBusiness.getArn(), null);

    assertThat(renewed.getFlags().businessType()).isEqualTo(BusinessType.RENEWAL);
    assertThat(fresh.getFlags().businessType()).isEqualTo(BusinessType.NEW_BUSINESS);
    assertThat(
            jdbc.queryForObject(
                "select business_type from bkg_invoice where invoice_no = ?",
                String.class,
                renewed.getInvoiceNo()))
        .isEqualTo("RENEWAL");
    assertThat(events.forInvoice(renewed.getInvoiceNo()).orElseThrow().businessType())
        .isEqualTo(BusinessType.RENEWAL);
    assertThat(events.forInvoice(fresh.getInvoiceNo()).orElseThrow().businessType())
        .isEqualTo(BusinessType.NEW_BUSINESS);
  }

  @Test
  void billingNumberIsRequiredForTheConfiguredLinesAndUniquePerInsurer() {
    Account first = fx.motor();
    Account second = fx.motor();
    String billingNo = "INSA-BILL-" + BookingFixtures.token();
    parameters.update(BookingSettings.BILLING_NO_LINES, DEFAULT_LINES + "," + first.getLineCode());
    try {
      assertThatThrownBy(() -> book(first.getArn(), null))
          .extracting("code")
          .isEqualTo(InsurerBillingNumbers.REQUIRED);

      BookedInvoice invoice = book(first.getArn(), billingNo);
      assertThat(invoice.getInsurerBillingNo()).isEqualTo(billingNo);
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from audit_log where entity_id = ? and summary like ?",
                  Long.class,
                  invoice.getInvoiceNo(),
                  "%insurer billing no. " + billingNo + "%"))
          .isEqualTo(1L);
      assertThat(notices("proc", invoice.getInvoiceNo())).isPositive();

      assertThatThrownBy(() -> book(second.getArn(), billingNo))
          .extracting("code", "message")
          .containsExactly(
              InsurerBillingNumbers.DUPLICATE,
              "Billing number "
                  + billingNo
                  + " of "
                  + second.getInsurerCode()
                  + " is already on invoice "
                  + invoice.getInvoiceNo());
      assertThat(notices("proc", second.getArn())).isPositive();
    } finally {
      parameters.update(BookingSettings.BILLING_NO_LINES, DEFAULT_LINES);
    }
  }

  @Test
  void theBookingUploadTakesTheBillingNumber() {
    Account account = fx.motor();
    String billingNo = "UPL-BILL-" + BookingFixtures.token();
    BulkContext context =
        new BulkContext(fx.company(), "BLK-BILL", BookingFixtures.BOOKED_ON, Map.of());
    parameters.update(
        BookingSettings.BILLING_NO_LINES, DEFAULT_LINES + "," + account.getLineCode());
    try {
      assertThat(upload.validate(new BulkRow(1, Map.of("ARN", account.getArn())), context))
          .containsExactly("Enter the insurer billing number");
      BulkRow row =
          new BulkRow(2, Map.of("ARN", account.getArn(), "Insurer Billing No", billingNo));
      assertThat(upload.validate(row, context)).isEmpty();
      String invoiceNo = as.run("proc", () -> upload.commit(row, context));
      assertThat(
              jdbc.queryForObject(
                  "select insurer_billing_no from bkg_invoice where invoice_no = ?",
                  String.class,
                  invoiceNo))
          .isEqualTo(billingNo);
    } finally {
      parameters.update(BookingSettings.BILLING_NO_LINES, DEFAULT_LINES);
    }
  }

  @Test
  void theNewBusinessReportsFilterByBusinessType() {
    Account renewal = fx.renewalMotor("ARN-2025-" + BookingFixtures.token());
    Account fresh = fx.motor();
    String renewed = book(renewal.getArn(), null).getInvoiceNo();
    String booked = book(fresh.getArn(), null).getInvoiceNo();

    assertThat(cells(run("ao", "NB-BOOKED-REG", "RENEWAL"), "invoice_no"))
        .contains(renewed)
        .doesNotContain(booked);
    assertThat(cells(run("ao", "NB-BOOKED-REG", "NEW_BUSINESS"), "invoice_no"))
        .contains(booked)
        .doesNotContain(renewed);
    assertThat(cells(run("ao", "NB-PLC-UPDATE", "RENEWAL", "arn", renewal.getArn()), "arn"))
        .containsExactly(renewal.getArn());
    assertThat(cells(run("ao", "NB-PLC-UPDATE", "NEW_BUSINESS", "arn", renewal.getArn()), "arn"))
        .isEmpty();
    long renewals = bookings(run("mkttl", "NB-PRODUCTION", "RENEWAL"));
    assertThat(renewals)
        .isPositive()
        .isLessThanOrEqualTo(bookings(run("mkttl", "NB-PRODUCTION", "ALL")));
  }

  private ReportResult run(String user, String code, String businessType, String... extra) {
    Map<String, String> params = new HashMap<>();
    params.put("companyId", fx.company().toString());
    params.put("from", "2026-01-01");
    params.put("to", "2026-12-31");
    params.put("businessType", businessType);
    for (int i = 0; i < extra.length; i += 2) {
      params.put(extra[i], extra[i + 1]);
    }
    return as.run(user, () -> reports.run(code, params));
  }

  private static List<Object> cells(ReportResult result, String key) {
    return result.rows().stream()
        .filter(r -> r.kind() == RowKind.DETAIL)
        .map(r -> r.cells().get(key))
        .toList();
  }

  private static long bookings(ReportResult result) {
    return cells(result, "bookings").stream().mapToLong(v -> ((Number) v).longValue()).sum();
  }

  private long notices(String user, String reference) {
    return jdbc.queryForObject(
        "select count(*) from msg_notification where lower(recipient) = ? and (entity_id = ?"
            + " or body like ? or title like ?)",
        Long.class,
        user,
        reference,
        "%" + reference + "%",
        "%" + reference + "%");
  }
}
