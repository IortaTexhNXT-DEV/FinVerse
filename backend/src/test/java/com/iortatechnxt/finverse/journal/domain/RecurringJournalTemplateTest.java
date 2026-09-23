package com.iortatechnxt.finverse.journal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.journal.api.dto.RecurringTemplateResponse;
import com.iortatechnxt.finverse.journal.domain.RecurringJournalTemplate.RecurringHeader;
import com.iortatechnxt.finverse.journal.domain.RecurringJournalTemplate.RecurringSchedule;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecurringJournalTemplateTest {

  private static final LocalDate SEP_1 = LocalDate.of(2026, 9, 1);
  private static final LocalDate SEP_15 = LocalDate.of(2026, 9, 15);
  private static final LocalDate OCT_15 = LocalDate.of(2026, 10, 15);

  private static RecurringJournalTemplate monthlyOn15th(LocalDate end) {
    return new RecurringJournalTemplate(
        1L,
        new RecurringHeader(1L, "Rent", JournalType.ACCRUAL, "PHP", "Rent", null, false, false),
        new RecurringSchedule(RecurrenceFrequency.MONTHLY, 15, SEP_1, end),
        List.of());
  }

  @Test
  void nextRunOfANewTemplateIsTheFirstOccurrenceStillToGenerateEvenWhenAlreadyDue() {
    RecurringJournalTemplate template = monthlyOn15th(null);

    // Created on 23 September: 15 September is due and not generated yet.
    assertThat(template.nextPendingOccurrence()).isEqualTo(SEP_15);
    assertThat(template.dueOccurrences(LocalDate.of(2026, 9, 23))).containsExactly(SEP_15);
    assertThat(RecurringTemplateResponse.from(template).nextOccurrence()).isEqualTo(SEP_15);
  }

  @Test
  void nextRunMovesOnOnlyWhenTheOccurrenceIsGenerated() {
    RecurringJournalTemplate template = monthlyOn15th(null);
    template.markGenerated(SEP_15);

    assertThat(template.nextPendingOccurrence()).isEqualTo(OCT_15);
  }

  @Test
  void endedOrInactiveTemplatesHaveNoNextRun() {
    RecurringJournalTemplate ended = monthlyOn15th(LocalDate.of(2026, 9, 30));
    ended.markGenerated(SEP_15);
    assertThat(ended.nextPendingOccurrence()).isNull();

    RecurringJournalTemplate inactive = monthlyOn15th(null);
    inactive.setActive(false);
    assertThat(RecurringTemplateResponse.from(inactive).nextOccurrence()).isNull();
  }
}
