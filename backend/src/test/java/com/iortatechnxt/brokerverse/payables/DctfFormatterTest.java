package com.iortatechnxt.brokerverse.payables;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.payables.domain.NotificationFormat;
import com.iortatechnxt.brokerverse.payables.service.NotificationRecord;
import com.iortatechnxt.brokerverse.payables.service.PaymentNotificationFormatter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The Direct Credit Transaction File of Disbursement (DIS 2.16.1, Appendix B p.147). */
class DctfFormatterTest {

  private static final LocalDate DATE = LocalDate.of(2026, 9, 25);

  @Test
  void theDctfHasAHeaderAndFixedWidthCredits() {
    String text =
        PaymentNotificationFormatter.dctf(
            "DCTF_TEST.txt",
            DATE,
            List.of(
                new NotificationRecord(
                    "DV-2026-000001",
                    null,
                    DATE,
                    "CL-1",
                    "Juan dela Cruz",
                    "0012-3456-7890",
                    "PHP",
                    new BigDecimal("2500.5"))));
    String[] lines = text.split("\r\n");
    assertThat(lines).hasSize(2);
    assertThat(lines[0]).startsWith("09252026 DCTF_TEST.TXT");
    String detail = lines[1];
    assertThat(detail).hasSize(89);
    assertThat(detail.substring(0, 12)).isEqualTo("001234567890");
    assertThat(detail.substring(12, 42)).startsWith("JUAN DELA CRUZ");
    assertThat(detail.substring(42, 54)).isBlank();
    assertThat(detail.substring(54, 74)).startsWith("DV-2026-000001");
    assertThat(detail.substring(74)).isEqualTo("000000002500.50");
  }

  @Test
  void theDctfFormatIsChosenByTheBankAccount() {
    String text =
        PaymentNotificationFormatter.format(NotificationFormat.DCTF, "123", DATE, List.of());
    assertThat(text).startsWith("09252026 DCTF_20260925.TXT");
  }
}
