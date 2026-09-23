package com.iortatechnxt.finverse.journal.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Recurring (standing) journal template, e.g. monthly rent or an accrual that is reversed at the
 * start of the next period. Each occurrence generates a DRAFT journal (or submits it for approval
 * when {@code autoSubmit} is set); generation is idempotent per occurrence date.
 */
@Entity
@Table(name = "jnl_recurring_template")
public class RecurringJournalTemplate extends BaseEntity {

  private static final Set<JournalType> ALLOWED_TYPES =
      EnumSet.of(JournalType.MANUAL, JournalType.ADJUSTMENT, JournalType.ACCRUAL);

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "journal_type", nullable = false, length = 20)
  private JournalType journalType;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, length = 500)
  private String narration;

  @Column(length = 60)
  private String reference;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private RecurrenceFrequency frequency;

  @Column(name = "day_of_month", nullable = false)
  private int dayOfMonth;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "auto_reverse", nullable = false)
  private boolean autoReverse;

  @Column(name = "auto_submit", nullable = false)
  private boolean autoSubmit;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "last_occurrence_date")
  private LocalDate lastOccurrenceDate;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "jnl_recurring_line", joinColumns = @JoinColumn(name = "template_id"))
  @OrderColumn(name = "line_no")
  private final List<RecurringLine> lines = new ArrayList<>();

  protected RecurringJournalTemplate() {}

  /**
   * Creates a template.
   *
   * @param companyId company
   * @param header header values
   * @param schedule schedule
   * @param lines lines
   */
  public RecurringJournalTemplate(
      Long companyId,
      RecurringHeader header,
      RecurringSchedule schedule,
      List<RecurringLine> lines) {
    this.companyId = companyId;
    assign(header, schedule, lines);
  }

  /**
   * Changes the template. Occurrences already generated are not affected.
   *
   * @param header header values
   * @param schedule schedule
   * @param newLines lines
   */
  public void apply(
      RecurringHeader header, RecurringSchedule schedule, List<RecurringLine> newLines) {
    assign(header, schedule, newLines);
  }

  private void assign(
      RecurringHeader header, RecurringSchedule schedule, List<RecurringLine> newLines) {
    if (!ALLOWED_TYPES.contains(header.journalType())) {
      throw new BusinessRuleException(
          "INVALID_JOURNAL_TYPE", "Recurring journals must be MANUAL, ADJUSTMENT or ACCRUAL");
    }
    if (schedule.endDate() != null && schedule.endDate().isBefore(schedule.startDate())) {
      throw new BusinessRuleException("INVALID_SCHEDULE", "End date is before the start date");
    }
    this.branchId = header.branchId();
    this.name = header.name();
    this.journalType = header.journalType();
    this.currency = header.currency();
    this.narration = header.narration();
    this.reference = header.reference();
    this.autoReverse = header.autoReverse();
    this.autoSubmit = header.autoSubmit();
    this.frequency = schedule.frequency();
    this.dayOfMonth = schedule.dayOfMonth();
    this.startDate = schedule.startDate();
    this.endDate = schedule.endDate();
    this.lines.clear();
    this.lines.addAll(newLines);
  }

  /**
   * Occurrences due up to a date that were not generated yet (catch-up after downtime included).
   *
   * @param asOf run date
   * @return due dates, ascending
   */
  public List<LocalDate> dueOccurrences(LocalDate asOf) {
    if (!active) {
      return List.of();
    }
    return frequency.occurrences(dayOfMonth, startDate, endDate, asOf).stream()
        .filter(d -> lastOccurrenceDate == null || d.isAfter(lastOccurrenceDate))
        .toList();
  }

  /**
   * Next occurrence after a date.
   *
   * @param after reference date
   * @return next date or null when the schedule has ended
   */
  public LocalDate nextOccurrence(LocalDate after) {
    return frequency.nextAfter(dayOfMonth, startDate, endDate, after);
  }

  /**
   * Records that an occurrence was generated.
   *
   * @param date occurrence date
   */
  public void markGenerated(LocalDate date) {
    if (lastOccurrenceDate == null || date.isAfter(lastOccurrenceDate)) {
      this.lastOccurrenceDate = date;
    }
  }

  /**
   * Activates or deactivates the template.
   *
   * @param value new state
   */
  public void setActive(boolean value) {
    this.active = value;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getName() {
    return name;
  }

  public JournalType getJournalType() {
    return journalType;
  }

  public String getCurrency() {
    return currency;
  }

  public String getNarration() {
    return narration;
  }

  public String getReference() {
    return reference;
  }

  public RecurrenceFrequency getFrequency() {
    return frequency;
  }

  public int getDayOfMonth() {
    return dayOfMonth;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public boolean isAutoReverse() {
    return autoReverse;
  }

  public boolean isAutoSubmit() {
    return autoSubmit;
  }

  public boolean isActive() {
    return active;
  }

  public LocalDate getLastOccurrenceDate() {
    return lastOccurrenceDate;
  }

  public List<RecurringLine> getLines() {
    return List.copyOf(lines);
  }

  /**
   * Header values of a template.
   *
   * @param branchId branch
   * @param name unique name within the company
   * @param journalType MANUAL, ADJUSTMENT or ACCRUAL
   * @param currency header currency
   * @param narration narration
   * @param reference reference
   * @param autoReverse generate a reversing draft dated the first day of the next period
   * @param autoSubmit submit generated journals for approval instead of leaving drafts
   */
  public record RecurringHeader(
      Long branchId,
      String name,
      JournalType journalType,
      String currency,
      String narration,
      String reference,
      boolean autoReverse,
      boolean autoSubmit) {}

  /**
   * Schedule of a template.
   *
   * @param frequency frequency
   * @param dayOfMonth day of month (31 = month end)
   * @param startDate first possible occurrence
   * @param endDate last possible occurrence (null = open ended)
   */
  public record RecurringSchedule(
      RecurrenceFrequency frequency, int dayOfMonth, LocalDate startDate, LocalDate endDate) {}
}
