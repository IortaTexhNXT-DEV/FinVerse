package com.iortatechnxt.brokerverse.configpromo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.configpromo.service.ChangeWindow;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** The change window of production imports. */
class ChangeWindowTest {

  /** Saturday 10 October 2026. */
  private static final LocalDateTime SATURDAY = LocalDateTime.of(2026, 10, 10, 0, 0);

  @Test
  void anOvernightWindowRunsIntoTheNextDay() {
    ChangeWindow window = ChangeWindow.parse("SAT-SUN 20:00-06:00");

    assertThat(window.days()).containsExactlyInAnyOrder(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    assertThat(window.isOpen(SATURDAY.withHour(21))).isTrue();
    assertThat(window.isOpen(SATURDAY.withHour(12))).isFalse();
    assertThat(window.isOpen(SATURDAY.plusDays(1).withHour(5))).isTrue();
    assertThat(window.isOpen(SATURDAY.plusDays(2).withHour(5))).isTrue();
    assertThat(window.isOpen(SATURDAY.plusDays(2).withHour(7))).isFalse();
    assertThat(window.isOpen(SATURDAY.minusDays(1).withHour(21))).isFalse();
  }

  @Test
  void aDaytimeWindowOnListedDays() {
    ChangeWindow window = ChangeWindow.parse("FRI,SUN 08:00-17:00");

    assertThat(window.isOpen(SATURDAY.minusDays(1).withHour(9))).isTrue();
    assertThat(window.isOpen(SATURDAY.withHour(9))).isFalse();
    assertThat(window.isOpen(SATURDAY.plusDays(1).withHour(17))).isTrue();
    assertThat(window.isOpen(SATURDAY.plusDays(1).withHour(18))).isFalse();
  }

  @Test
  void aWindowThatCannotBeReadIsRefusedWithItsFormat() {
    assertThatThrownBy(() -> ChangeWindow.parse("weekends"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("SAT-SUN 20:00-06:00");
    assertThatThrownBy(() -> ChangeWindow.parse("XYZ 20:00-06:00"))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> ChangeWindow.parse("SAT 25:00-06:00"))
        .isInstanceOf(BusinessRuleException.class);
  }
}
