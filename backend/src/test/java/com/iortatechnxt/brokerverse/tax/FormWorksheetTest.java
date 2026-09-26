package com.iortatechnxt.brokerverse.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificate;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificate.Facts;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificateLine;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificateLine.Kind;
import com.iortatechnxt.brokerverse.tax.service.BirOutputQueries.FormLineDef;
import com.iortatechnxt.brokerverse.tax.service.FormWorksheetService;
import com.iortatechnxt.brokerverse.tax.service.FormWorksheetService.LineValue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The form worksheet lines (1702-Q / 1702) and the received certificate (FRBS 3.2.0, DIS 2.11). */
class FormWorksheetTest {

  @Test
  void theIncomeTaxWorksheetAddsLinesAppliesTheRateAndCreditsTheCertificates() {
    List<FormLineDef> defs =
        List.of(
            new FormLineDef(10, "Income", "ACCOUNT_CREDITS", List.of("41"), null),
            new FormLineDef(20, "Unmapped", "ACCOUNT_CREDITS", List.of(), null),
            new FormLineDef(40, "Expenses", "ACCOUNT_DEBITS", List.of("5"), null),
            new FormLineDef(50, "Taxable", "LINES", List.of(), "10, 20, -40"),
            new FormLineDef(60, "Tax due", "RATE", List.of(), "50:RCIT"),
            new FormLineDef(70, "Credits", "CERTIFICATES", List.of(), null),
            new FormLineDef(80, "Payable", "LINES", List.of(), "60,-70"));
    List<LineValue> lines =
        FormWorksheetService.calculate(
            defs,
            d -> "Income".equals(d.label()) ? new BigDecimal("1000") : new BigDecimal("400"),
            new BigDecimal("20"),
            key -> new BigDecimal("25"));
    assertThat(lines)
        .extracting(LineValue::amount)
        .containsExactly(
            new BigDecimal("1000.00"),
            new BigDecimal("0.00"),
            new BigDecimal("400.00"),
            new BigDecimal("600.00"),
            new BigDecimal("150.00"),
            new BigDecimal("20.00"),
            new BigDecimal("130.00"));
    assertThat(lines.get(1).note()).contains("AQ07");
    List<LineValue> loss =
        FormWorksheetService.calculate(
            List.of(
                new FormLineDef(10, "Loss", "LINES", List.of(), "-20"),
                new FormLineDef(20, "Tax", "RATE", List.of(), "10:RCIT")),
            d -> BigDecimal.ZERO,
            BigDecimal.ZERO,
            key -> new BigDecimal("25"));
    assertThat(loss.get(1).amount()).isEqualByComparingTo("0");
    assertThatThrownBy(
            () ->
                FormWorksheetService.calculate(
                    List.of(new FormLineDef(1, "X", "OTHER", List.of(), null)),
                    d -> BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    key -> BigDecimal.ZERO))
        .hasMessageContaining("OTHER");
  }

  @Test
  void aReceivedCertificateTotalsItsLinesByKindAndIsCancelledOnce() {
    ReceivedCertificate c =
        new ReceivedCertificate(
            1L,
            new Facts(
                "2307-1",
                "INS-1",
                "Insurer",
                null,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 9, 20),
                "TAX",
                null,
                null),
            List.of(
                new ReceivedCertificateLine(
                    1,
                    Kind.COMMISSION,
                    "WC158",
                    null,
                    new BigDecimal("1000"),
                    new BigDecimal("20")),
                new ReceivedCertificateLine(
                    2,
                    Kind.INCENTIVE,
                    "WC158",
                    null,
                    new BigDecimal("500"),
                    new BigDecimal("10"))));
    assertThat(c.getIncomeTotal()).isEqualByComparingTo("1500");
    assertThat(c.getTaxTotal()).isEqualByComparingTo("30");
    assertThat(c.taxOf(Kind.INCENTIVE)).isEqualByComparingTo("10");
    c.cancel("Duplicate", null);
    assertThat(c.getStatus()).isEqualTo(ReceivedCertificate.CANCELLED);
    assertThatThrownBy(() -> c.cancel("Again", null)).hasMessageContaining("already");
  }
}
