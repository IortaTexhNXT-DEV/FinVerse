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
import java.util.Arrays;
import java.util.List;

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

  @Column(name = "report_code", nullable = false, length = 40, updatable = false)
  private String reportCode;

  @Column(nullable = false, length = 50, updatable = false)
  private String username;

  @Column(nullable = false, length = 2000)
  private String parameters;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
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
    if (!active) {
      return false;
    }
    return switch (frequency) {
      case DAILY -> true;
      case WEEKLY -> date.getDayOfWeek().getValue() == dayOfWeek;
      case MONTHLY -> date.getDayOfMonth() == Math.min(dayOfMonth, date.lengthOfMonth());
    };
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

  /** How often a subscription runs. */
  public enum Frequency {
    /** Every day. */
    DAILY,
    /** Once a week. */
    WEEKLY,
    /** Once a month. */
    MONTHLY
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
   */
  public record Schedule(
      String parameters,
      Frequency frequency,
      DayOfWeek dayOfWeek,
      Integer dayOfMonth,
      String format,
      List<String> recipients) {

    /** Defensive copy. */
    public Schedule {
      recipients = recipients == null ? List.of() : List.copyOf(recipients);
    }
  }
}
