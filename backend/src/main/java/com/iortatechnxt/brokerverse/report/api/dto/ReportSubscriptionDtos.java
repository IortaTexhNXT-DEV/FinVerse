package com.iortatechnxt.brokerverse.report.api.dto;

import com.iortatechnxt.brokerverse.report.core.ReportSubscriptionService;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscription;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscription.Frequency;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscriptionRun;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscriptionRun.Outcome;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Requests and responses of the report subscriptions (G04). */
public final class ReportSubscriptionDtos {

  private ReportSubscriptionDtos() {}

  /**
   * New or changed subscription.
   *
   * @param reportCode report (on creation)
   * @param parameters report parameters by name
   * @param frequency DAILY, WEEKLY or MONTHLY
   * @param dayOfWeek day of a weekly run
   * @param dayOfMonth day of a monthly run
   * @param format file format
   * @param recipients e-mail addresses
   */
  public record SubscriptionRequest(
      @Size(max = 40) String reportCode,
      Map<String, String> parameters,
      @NotNull Frequency frequency,
      DayOfWeek dayOfWeek,
      @Min(1) @Max(31) Integer dayOfMonth,
      @NotNull ExportFormat format,
      @NotEmpty @Size(max = 20) List<@Email @Size(max = 120) String> recipients) {

    /**
     * The service request.
     *
     * @return request
     */
    public ReportSubscriptionService.Request toRequest() {
      return new ReportSubscriptionService.Request(
          parameters, frequency, dayOfWeek, dayOfMonth, format, recipients);
    }
  }

  /**
   * A subscription.
   *
   * @param id id
   * @param reportCode report
   * @param username subscriber
   * @param parameters parameters by name
   * @param frequency frequency
   * @param dayOfWeek day of a weekly run (1 Monday to 7 Sunday)
   * @param dayOfMonth day of a monthly run
   * @param format file format
   * @param recipients e-mail addresses
   * @param active whether it still runs
   * @param lastRunDate date of the last run
   * @param createdAt subscription time
   */
  public record SubscriptionResponse(
      Long id,
      String reportCode,
      String username,
      Map<String, String> parameters,
      Frequency frequency,
      Integer dayOfWeek,
      Integer dayOfMonth,
      String format,
      List<String> recipients,
      boolean active,
      LocalDate lastRunDate,
      Instant createdAt) {

    /**
     * Maps a subscription.
     *
     * @param s subscription
     * @param parameters its parameters
     * @return response
     */
    public static SubscriptionResponse from(ReportSubscription s, Map<String, String> parameters) {
      return new SubscriptionResponse(
          s.getId(),
          s.getReportCode(),
          s.getUsername(),
          parameters,
          s.getFrequency(),
          s.getDayOfWeek(),
          s.getDayOfMonth(),
          s.getFormat(),
          s.getRecipientList(),
          s.isActive(),
          s.getLastRunDate(),
          s.getCreatedAt());
    }
  }

  /**
   * A run of a subscription.
   *
   * @param id id
   * @param runDate business date
   * @param status outcome
   * @param reportRunId archived report run (download from the report archive)
   * @param rowCount rows
   * @param detail file sent, or reason of a failure or refusal
   * @param runAt run time
   */
  public record RunResponse(
      Long id,
      LocalDate runDate,
      Outcome status,
      Long reportRunId,
      int rowCount,
      String detail,
      Instant runAt) {

    /**
     * Maps a run.
     *
     * @param r run
     * @return response
     */
    public static RunResponse from(ReportSubscriptionRun r) {
      return new RunResponse(
          r.getId(),
          r.getRunDate(),
          r.getStatus(),
          r.getReportRunId(),
          r.getRowCount(),
          r.getDetail(),
          r.getCreatedAt());
    }
  }

  /**
   * Whether a report code was given.
   *
   * @param request request
   * @return true when present
   */
  public static boolean hasReport(SubscriptionRequest request) {
    return request.reportCode() != null && !request.reportCode().isBlank();
  }
}
