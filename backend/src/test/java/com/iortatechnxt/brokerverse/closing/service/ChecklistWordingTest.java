package com.iortatechnxt.brokerverse.closing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.period.domain.PeriodStatus;
import org.junit.jupiter.api.Test;

/** The closing checklist names the status of a period in words, never its code. */
class ChecklistWordingTest {

  @Test
  void aStatusReadsInWords() {
    assertThat(ClosingChecklistService.word(PeriodStatus.OPEN)).isEqualTo("open");
    for (PeriodStatus status : PeriodStatus.values()) {
      assertThat(ClosingChecklistService.word(status)).doesNotContain("_").isLowerCase();
    }
  }
}
