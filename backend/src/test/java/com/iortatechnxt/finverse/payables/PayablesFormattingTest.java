package com.iortatechnxt.finverse.payables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.payables.domain.NotificationFormat;
import com.iortatechnxt.finverse.payables.report.AgeingSlots;
import com.iortatechnxt.finverse.payables.service.NotificationRecord;
import com.iortatechnxt.finverse.payables.service.PaymentNotificationFormatter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PayablesFormattingTest {

  @Test
  void ageingSlotsBucketAgesAndLabelBuckets() {
    AgeingSlots slots = AgeingSlots.of(AgeingSlots.DEFAULT);
    assertThat(slots.size()).isEqualTo(5);
    assertThat(slots.index(-10)).isZero();
    assertThat(slots.index(30)).isZero();
    assertThat(slots.index(31)).isEqualTo(1);
    assertThat(slots.index(120)).isEqualTo(3);
    assertThat(slots.index(121)).isEqualTo(4);
    assertThat(slots.label(0)).isEqualTo("0-30");
    assertThat(slots.label(1)).isEqualTo("31-60");
    assertThat(slots.label(4)).isEqualTo("Over 120");
    assertThat(slots.columns()).hasSize(5);
    assertThat(slots.describe()).isEqualTo("30/60/90/120");
    assertThat(AgeingSlots.parameters()).hasSize(5);
    assertThatThrownBy(() -> AgeingSlots.of(List.of())).isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> AgeingSlots.of(List.of(30, 30)))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> AgeingSlots.of(List.of(1, 2, 3, 4, 5, 6)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void notificationFileFollowsTheDocumentedLayouts() {
    List<NotificationRecord> records =
        List.of(
            new NotificationRecord(
                "PV-HO-2026-000001",
                "100001",
                LocalDate.of(2026, 9, 1),
                "S-0001",
                "Metro Office Supplies, Co.",
                "123-456",
                "PHP",
                new BigDecimal("1234.50")),
            new NotificationRecord(
                "PV-HO-2026-000002",
                null,
                LocalDate.of(2026, 9, 2),
                "S-0002",
                "Cloud \"Systems\" Ñ",
                null,
                "PHP",
                new BigDecimal("10.00")));
    String fixed =
        PaymentNotificationFormatter.format(
            NotificationFormat.FIXED_WIDTH, "0012-3456", LocalDate.of(2026, 9, 2), records);
    List<String> lines = fixed.lines().toList();
    assertThat(lines).hasSize(3);
    assertThat(lines.get(0)).hasSize(163).startsWith("010012-3456");
    assertThat(lines.get(0).substring(62, 70)).isEqualTo("01092026");
    assertThat(lines.get(0)).endsWith("000000000123450");
    assertThat(lines.get(1).substring(85, 125)).startsWith("CLOUD \"SYSTEMS\"  ");
    assertThat(lines.get(2))
        .hasSize(54)
        .isEqualTo("02" + "0012-3456           " + "02092026" + "000002" + "000000000000124450");

    String csv =
        PaymentNotificationFormatter.format(
            NotificationFormat.CSV, "0012-3456", LocalDate.of(2026, 9, 2), records);
    assertThat(csv.lines().toList())
        .hasSize(4)
        .contains(
            "01,0012-3456,PV-HO-2026-000001,100001,2026-09-01,S-0001,"
                + "\"Metro Office Supplies, Co.\",123-456,PHP,1234.50")
        .contains("02,0012-3456,2026-09-02,2,1244.50");
  }
}
