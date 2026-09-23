package com.iortatechnxt.finverse.journal.api.dto;

import com.iortatechnxt.finverse.journal.domain.JournalType;
import com.iortatechnxt.finverse.journal.domain.RecurrenceFrequency;
import com.iortatechnxt.finverse.journal.domain.RecurringJournalTemplate;
import com.iortatechnxt.finverse.journal.service.RecurringLines;
import java.time.LocalDate;
import java.util.List;

/**
 * Recurring journal template view.
 *
 * @param id id
 * @param companyId company
 * @param branchId branch
 * @param name name
 * @param journalType type
 * @param currency currency
 * @param narration narration
 * @param reference reference
 * @param frequency frequency
 * @param dayOfMonth day of month
 * @param startDate start
 * @param endDate end
 * @param autoReverse auto-reverse flag
 * @param autoSubmit auto-submit flag
 * @param active active flag
 * @param lastOccurrenceDate last generated occurrence
 * @param nextOccurrence next occurrence after today (null when ended or inactive)
 * @param createdBy maker
 * @param lines lines
 */
public record RecurringTemplateResponse(
    Long id,
    Long companyId,
    Long branchId,
    String name,
    JournalType journalType,
    String currency,
    String narration,
    String reference,
    RecurrenceFrequency frequency,
    int dayOfMonth,
    LocalDate startDate,
    LocalDate endDate,
    boolean autoReverse,
    boolean autoSubmit,
    boolean active,
    LocalDate lastOccurrenceDate,
    LocalDate nextOccurrence,
    String createdBy,
    List<JournalLineRequest> lines) {

  /**
   * Maps an entity.
   *
   * @param t template
   * @param today business date used for the next occurrence
   * @return response
   */
  public static RecurringTemplateResponse from(RecurringJournalTemplate t, LocalDate today) {
    LocalDate after =
        t.getLastOccurrenceDate() != null && t.getLastOccurrenceDate().isAfter(today)
            ? t.getLastOccurrenceDate()
            : today;
    return new RecurringTemplateResponse(
        t.getId(),
        t.getCompanyId(),
        t.getBranchId(),
        t.getName(),
        t.getJournalType(),
        t.getCurrency(),
        t.getNarration(),
        t.getReference(),
        t.getFrequency(),
        t.getDayOfMonth(),
        t.getStartDate(),
        t.getEndDate(),
        t.isAutoReverse(),
        t.isAutoSubmit(),
        t.isActive(),
        t.getLastOccurrenceDate(),
        t.isActive() ? t.nextOccurrence(after) : null,
        t.getCreatedBy(),
        t.getLines().stream().map(RecurringLines::toRequest).toList());
  }
}
