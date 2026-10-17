package com.iortatechnxt.brokerverse.placement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.bulk.service.BulkFileReader;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReportKind;
import com.iortatechnxt.brokerverse.placement.domain.ReportedPayment;
import com.iortatechnxt.brokerverse.placement.service.OdsSpreadsheetWriter;
import com.iortatechnxt.brokerverse.placement.service.PaymentReportParser;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The CLPC billing file (.ods) and the payment report reader, without Spring. */
class PlacementFilesTest {

  private final BulkFileReader reader = new BulkFileReader();

  private ParsedFile csv(String text) {
    return reader.read("report.csv", text.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void clpcReportsAreReadByPnOrLoanNumberWithStatusAmountAndDate() {
    List<ReportedPayment> rows =
        PaymentReportParser.read(
            csv(
                "PN No.,Loan Application No.,Status,Amount Paid,Payment Date\n"
                    + "pn-1,,PAID,\"1,250.50\",2026-09-10\n"
                    + ",HL-9,unpaid,,\n"
                    + ",,PAID,10,2026-09-11\n"),
            PaymentReportKind.CLPC);
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0))
        .isEqualTo(
            new ReportedPayment(
                2, "PN-1", true, new BigDecimal("1250.50"), LocalDate.of(2026, 9, 10)));
    assertThat(rows.get(1).reference()).isEqualTo("HL-9");
    assertThat(rows.get(1).paid()).isFalse();
    assertThat(rows.get(1).amount()).isNull();
  }

  @Test
  void referenceReportsNeedAnArnColumnAndReadableValues() {
    List<ReportedPayment> rows =
        PaymentReportParser.read(csv("ARN\nARN-2026-000001\n"), PaymentReportKind.REFERENCE);
    assertThat(rows.get(0).paid()).isTrue();
    assertThatThrownBy(
            () -> PaymentReportParser.read(csv("PN No.\nX\n"), PaymentReportKind.REFERENCE))
        .extracting("code")
        .isEqualTo("PAYMENT_REPORT_LAYOUT");
    assertThatThrownBy(
            () ->
                PaymentReportParser.read(
                    csv("Reference,Amount\nA,abc\n"), PaymentReportKind.REFERENCE))
        .extracting("code")
        .isEqualTo("PAYMENT_REPORT_VALUE");
    assertThatThrownBy(
            () ->
                PaymentReportParser.read(
                    csv("Reference,Date\nA,10/09/2026\n"), PaymentReportKind.REFERENCE))
        .extracting("code")
        .isEqualTo("PAYMENT_REPORT_VALUE");
    assertThatThrownBy(
            () -> PaymentReportParser.read(csv("PN No.,Status\n,PAID\n"), PaymentReportKind.CLPC))
        .extracting("code")
        .isEqualTo("PAYMENT_REPORT_EMPTY");
    assertThatThrownBy(() -> PaymentReportParser.read(csv("PN No.\n"), PaymentReportKind.CLPC))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void theOdsBillingFileIsAReadableSpreadsheet() {
    byte[] ods =
        OdsSpreadsheetWriter.write(
            new SheetSpec(
                "CLPC <Billing>",
                List.of("PN No.", "Premium", "Booking Date", "Amortised", "Borrower"),
                List.of(
                    Arrays.<Object>asList(
                        "PN-1",
                        new BigDecimal("1500.25"),
                        LocalDate.of(2026, 9, 1),
                        true,
                        "A & B \"Co\""),
                    Arrays.<Object>asList("PN-2", 10, null, false, "C"))));
    assertThat(new String(ods, 0, 2, StandardCharsets.US_ASCII)).isEqualTo("PK");
    ParsedFile parsed = reader.read("billing.ods", ods);
    assertThat(parsed.headers())
        .containsExactly("PN No.", "Premium", "Booking Date", "Amortised", "Borrower");
    assertThat(parsed.rows()).hasSize(2);
    assertThat(parsed.rows().get(0).values())
        .containsEntry("PN No.", "PN-1")
        .containsEntry("Amortised", "Y")
        .containsEntry("Borrower", "A & B \"Co\"");
    assertThat(parsed.rows().get(1).values()).containsEntry("Amortised", "N");
  }
}
