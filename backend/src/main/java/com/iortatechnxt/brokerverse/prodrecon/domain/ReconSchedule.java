package com.iortatechnxt.brokerverse.prodrecon.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.Frequency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Extraction schedule of an insurer's production register (PRCID.001): frequency, run day and the
 * next run date (rolled to the next working day by the job). The frequency per insurer is parked
 * with BDOI (OQ29); every schedule is maintained on the Schedules screen.
 */
@Entity
@Table(name = "prc_schedule")
public class ReconSchedule extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Frequency frequency;

  @Column(name = "run_day", nullable = false)
  private int runDay;

  @Column(name = "next_run_date", nullable = false)
  private LocalDate nextRunDate;

  @Column(name = "auto_send", nullable = false)
  private boolean autoSend;

  @Column(length = 500)
  private String recipients;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "last_run_at")
  private Instant lastRunAt;

  @Column(name = "last_extract_no", length = 30)
  private String lastExtractNo;

  protected ReconSchedule() {}

  /**
   * A new schedule.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param terms frequency, day, sending and activation
   * @param today date the first run is computed from
   */
  public ReconSchedule(Long companyId, String insurerCode, Terms terms, LocalDate today) {
    this.companyId = companyId;
    this.insurerCode = insurerCode;
    update(terms, today);
  }

  /**
   * Changes the schedule; the next run is computed again from today.
   *
   * @param terms frequency, day, sending and activation
   * @param today today
   */
  public final void update(Terms terms, LocalDate today) {
    this.frequency = terms.frequency();
    this.runDay = terms.runDay();
    this.autoSend = terms.autoSend();
    this.recipients = terms.recipients();
    this.active = terms.active();
    this.nextRunDate = nextOnOrAfter(today);
  }

  /**
   * Records a run and moves the next run date past it.
   *
   * @param extractNo extract produced, may be null
   * @param at time of the run
   * @param businessDate business date of the run
   */
  public void ran(String extractNo, Instant at, LocalDate businessDate) {
    this.lastRunAt = at;
    this.lastExtractNo = extractNo;
    this.nextRunDate = nextOnOrAfter(businessDate.plusDays(1));
  }

  /**
   * Moves the next run date to a later working day (holiday roll, PRCID.001).
   *
   * @param date working day
   */
  public void rollTo(LocalDate date) {
    this.nextRunDate = date;
  }

  /**
   * The first run date on or after a date.
   *
   * @param from first possible date
   * @return run date
   */
  private LocalDate nextOnOrAfter(LocalDate from) {
    if (frequency == Frequency.WEEKLY) {
      return from.with(
          TemporalAdjusters.nextOrSame(
              DayOfWeek.of(Math.min(runDay, DayOfWeek.SUNDAY.getValue()))));
    }
    LocalDate thisMonth = from.withDayOfMonth(runDay);
    return thisMonth.isBefore(from) ? thisMonth.plusMonths(1) : thisMonth;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public Frequency getFrequency() {
    return frequency;
  }

  public int getRunDay() {
    return runDay;
  }

  public LocalDate getNextRunDate() {
    return nextRunDate;
  }

  public boolean isAutoSend() {
    return autoSend;
  }

  public String getRecipients() {
    return recipients;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getLastRunAt() {
    return lastRunAt;
  }

  public String getLastExtractNo() {
    return lastExtractNo;
  }

  /**
   * Schedule terms.
   *
   * @param frequency monthly or weekly
   * @param runDay day of the month (1-28) or of the week (1 = Monday)
   * @param autoSend send the extract to the recipients right away
   * @param recipients e-mail addresses, comma separated, may be null
   * @param active whether the job runs it
   */
  public record Terms(
      Frequency frequency, int runDay, boolean autoSend, String recipients, boolean active) {}
}
