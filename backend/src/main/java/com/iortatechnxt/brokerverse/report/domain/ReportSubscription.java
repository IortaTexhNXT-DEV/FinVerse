package com.iortatechnxt.brokerverse.report.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * A user's subscription to a report (G04): the report with its parameters, run daily, weekly (on a
 * day of the week) or monthly (on a day of the month, the last day of a shorter month), rendered in
 * a format and e-mailed to the recipients.
 */
@Entity
@Table(name = "report_subscription")
public class ReportSubscription extends BaseEntity {

  private static final String SEPARATOR = ",";
  private static final int LAST_DAY = 31;

  /** Time of a run when the subscription names none (business time). */
  public static final LocalTime DEFAULT_RUN_TIME = LocalTime.of(6, 0);

  @Column(name = "report_code", nullable = false, length = 40, updatable = false)
  private String reportCode;

  @Column(nullable = false, length = 50, updatable = false)
  private String username;

  @Column(nullable = false, length = 2000)
  private String parameters;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Frequency frequency;

  @Column(name = "day_of_week")
  private Integer dayOfWeek;

  @Column(name = "day_of_month")
  private Integer dayOfMonth;

  @Column(nullable = false, length = 10)
  private String format;

  @Column(nullable = false, length = 1000)
  private String recipients;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "last_run_date")
  private LocalDate lastRunDate;

  @Column(name = "run_time")
  private LocalTime runTime;

  @Enumerated(EnumType.STRING)
  @Column(name = "date_period", length = 20)
  private RelativePeriod datePeriod;

  protected ReportSubscription() {}

  /**
   * Subscribes a user to a report.
   *
   * @param reportCode report
   * @param username subscriber
   * @param schedule parameters, frequency, format and recipients
   */
  public ReportSubscription(String reportCode, String username, Schedule schedule) {
    this.reportCode = reportCode;
    this.username = username;
    apply(schedule);
  }

  /**
   * Changes the subscription.
   *
   * @param schedule parameters, frequency, format and recipients
   */
  public void update(Schedule schedule) {
    apply(schedule);
  }

  private static void validate(Schedule s) {
    if (s.recipients().isEmpty()) {
      throw new BusinessRuleException("RECIPIENTS_REQUIRED", "Name at least one recipient");
    }
    if (s.frequency() == Frequency.WEEKLY && s.dayOfWeek() == null) {
      throw new BusinessRuleException("DAY_REQUIRED", "Choose the day of the week");
    }
    boolean validDay = s.dayOfMonth() != null && s.dayOfMonth() >= 1 && s.dayOfMonth() <= LAST_DAY;
    if (s.frequency() == Frequency.MONTHLY && !validDay) {
      throw new BusinessRuleException("DAY_REQUIRED", "Choose a day of the month from 1 to 31");
    }
  }

  private void apply(Schedule s) {
    validate(s);
    this.parameters = s.parameters();
    this.frequency = s.frequency();
    this.dayOfWeek = s.frequency() == Frequency.WEEKLY ? s.dayOfWeek().getValue() : null;
    this.dayOfMonth = s.frequency() == Frequency.MONTHLY ? s.dayOfMonth() : null;
    this.runTime = s.runTime();
    this.datePeriod = s.datePeriod();
    this.format = s.format();
    this.recipients = String.join(SEPARATOR, s.recipients());
  }

  /**
   * Whether the subscription runs on a date.
   *
   * @param date business date
   * @return true when active and due
   */
  public boolean isDueOn(LocalDate date) {
    return isDueOn(date, ReportSubscription::weekday);
  }

  /**
   * Whether the subscription runs on a business date, with the working days of its company for the
   * last working day of the month.
   *
   * @param date business date
   * @param working whether a date is a working day
   * @return true when active and due
   */
  public boolean isDueOn(LocalDate date, Predicate<LocalDate> working) {
    if (!active) {
      return false;
    }
    return switch (frequency) {
      case DAILY -> true;
      case WEEKLY -> date.getDayOfWeek().getValue() == dayOfWeek;
      case MONTHLY -> date.getDayOfMonth() == Math.min(dayOfMonth, date.lengthOfMonth());
      case LAST_WORKING_DAY -> working.test(date) && lastWorkingDay(date, working);
    };
  }

  /**
   * Whether the time of the run has come on its day.
   *
   * @param now business time of day
   * @return true at or after the run time (06:00 when none is set)
   */
  public boolean isTimeReached(LocalTime now) {
    return !now.isBefore(runTime == null ? DEFAULT_RUN_TIME : runTime);
  }

  private static boolean lastWorkingDay(LocalDate date, Predicate<LocalDate> working) {
    for (LocalDate d = date.plusDays(1); d.getMonth() == date.getMonth(); d = d.plusDays(1)) {
      if (working.test(d)) {
        return false;
      }
    }
    return true;
  }

  private static boolean weekday(LocalDate d) {
    return d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY;
  }

  /**
   * Records the date of the last run.
   *
   * @param date business date
   */
  public void ran(LocalDate date) {
    this.lastRunDate = date;
  }

  /** Stops the subscription; its runs stay. */
  public void deactivate() {
    this.active = false;
  }

  public String getReportCode() {
    return reportCode;
  }

  public String getUsername() {
    return username;
  }

  public String getParameters() {
    return parameters;
  }

  public Frequency getFrequency() {
    return frequency;
  }

  public Integer getDayOfWeek() {
    return dayOfWeek;
  }

  public Integer getDayOfMonth() {
    return dayOfMonth;
  }

  public String getFormat() {
    return format;
  }

  /**
   * E-mail recipients.
   *
   * @return addresses
   */
  public List<String> getRecipientList() {
    return Arrays.asList(recipients.split(SEPARATOR));
  }

  public boolean isActive() {
    return active;
  }

  public LocalDate getLastRunDate() {
    return lastRunDate;
  }

  public LocalTime getRunTime() {
    return runTime;
  }

  public RelativePeriod getDatePeriod() {
    return datePeriod;
  }

  /** How often a subscription runs. */
  public enum Frequency {
    /** Every day. */
    DAILY,
    /** Once a week. */
    WEEKLY,
    /** Once a month. */
    MONTHLY,
    /** On the last working day of every month. */
    LAST_WORKING_DAY
  }

  /**
   * What a subscription runs and where it sends the file.
   *
   * @param parameters report parameters (JSON object of names and values)
   * @param frequency frequency
   * @param dayOfWeek day of a weekly run
   * @param dayOfMonth day of a monthly run (1 to 31)
   * @param format file format
   * @param recipients e-mail addresses
   * @param runTime time of day of the run (business time), null for the default
   * @param datePeriod period of the date parameters, null to keep them as entered
   */
  public record Schedule(
      String parameters,
      Frequency frequency,
      DayOfWeek dayOfWeek,
      Integer dayOfMonth,
      String format,
      List<String> recipients,
      LocalTime runTime,
      RelativePeriod datePeriod) {

    /** Defensive copy. */
    public Schedule {
      recipients = recipients == null ? List.of() : List.copyOf(recipients);
    }

    /**
     * A schedule at the default time with the dates as entered.
     *
     * @param parameters report parameters
     * @param frequency frequency
     * @param dayOfWeek day of a weekly run
     * @param dayOfMonth day of a monthly run
     * @param format file format
     * @param recipients e-mail addresses
     */
    public Schedule(
        String parameters,
        Frequency frequency,
        DayOfWeek dayOfWeek,
        Integer dayOfMonth,
        String format,
        List<String> recipients) {
      this(parameters, frequency, dayOfWeek, dayOfMonth, format, recipients, null, null);
    }
  }
}
