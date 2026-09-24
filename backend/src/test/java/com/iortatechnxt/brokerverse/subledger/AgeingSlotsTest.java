package com.iortatechnxt.brokerverse.subledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.subledger.service.AgeingSlots;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgeingSlotsTest {

  @Test
  void bucketsAgesAndLabelsBuckets() {
    AgeingSlots slots = AgeingSlots.of(List.of(30, 60, 90, 120));
    assertThat(slots).isEqualTo(AgeingSlots.STANDARD);
    assertThat(slots.size()).isEqualTo(5);
    assertThat(slots.index(-10)).isZero();
    assertThat(slots.index(30)).isZero();
    assertThat(slots.index(31)).isEqualTo(1);
    assertThat(slots.index(120)).isEqualTo(3);
    assertThat(slots.index(121)).isEqualTo(4);
    assertThat(slots.label(0)).isEqualTo("0-30");
    assertThat(slots.label(4)).isEqualTo("Over 120");
    assertThat(slots.labels()).containsExactly("0-30", "31-60", "61-90", "91-120", "Over 120");
    assertThat(slots.describe()).isEqualTo("30/60/90/120");
    assertThat(slots.text()).isEqualTo("30,60,90,120");
  }

  @Test
  void parsesTextAndFallsBackWhenBlank() {
    assertThat(AgeingSlots.parse(null, AgeingSlots.STANDARD)).isSameAs(AgeingSlots.STANDARD);
    assertThat(AgeingSlots.parse("  ", AgeingSlots.STANDARD)).isSameAs(AgeingSlots.STANDARD);
    assertThat(AgeingSlots.parse("90; 180", AgeingSlots.STANDARD).labels())
        .containsExactly("0-90", "91-180", "Over 180");
    assertThat(AgeingSlots.parse("30 60,90", null).boundaries()).containsExactly(30, 60, 90);
  }

  @Test
  void rejectsInvalidSlots() {
    for (String bad : new String[] {"30,20", "0", "1,2,3,4,5,6", "x", ",", "123456"}) {
      assertThatThrownBy(() -> AgeingSlots.parse(bad, AgeingSlots.STANDARD))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("ascending positive day limits");
    }
    assertThatThrownBy(() -> AgeingSlots.of(List.of())).isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> AgeingSlots.of(List.of(30, 30)))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> AgeingSlots.of(List.of(1, 2, 3, 4, 5, 6)))
        .isInstanceOf(BusinessRuleException.class);
  }
}
