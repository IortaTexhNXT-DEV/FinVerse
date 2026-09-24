package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.core.ReportService.RenderedReport;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Daily adjustment report job (ADJID.016, {@code ADJ_DAILY_REPORT}, cron {@code
 * brokerverse.jobs.adj-daily-report-cron}, default 10:00 UTC = 18:00 PHT): exports the Adjustment
 * and Daily Endorsement Report of the business date for every company as Excel; the export is
 * archived with the Operations reports and the Adjustment team leaders are notified.
 */
@Component
public class AdjustmentDailyReportJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "ADJ_DAILY_REPORT";

  private static final String REPORT = "ADJ-DAILY";

  private final ReportService reports;
  private final OrganizationService organization;
  private final NotificationService notifications;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param reports report service (run, export and archive)
   * @param organization companies
   * @param notifications notifications
   * @param cron schedule
   */
  public AdjustmentDailyReportJob(
      ReportService reports,
      OrganizationService organization,
      NotificationService notifications,
      @Value("${brokerverse.jobs.adj-daily-report-cron:-}") String cron) {
    this.reports = reports;
    this.organization = organization;
    this.notifications = notifications;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Exports and archives the Adjustment and Daily Endorsement Report of the day (ADJID.016)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    Authentication previous = SecurityContextHolder.getContext().getAuthentication();
    SecurityContextHolder.getContext().setAuthentication(reportAuthority());
    try {
      List<Company> companies = organization.listCompanies();
      for (Company company : companies) {
        RenderedReport file =
            reports.export(
                REPORT,
                Map.of(
                    "companyId", String.valueOf(company.getId()),
                    "from", businessDate.toString(),
                    "to", businessDate.toString()),
                ExportFormat.XLSX);
        notifications.notifyPermission(
            "ADJ_APPROVE",
            new Notice(
                "Daily endorsement report " + businessDate,
                company.getName() + ": " + file.fileName() + " archived",
                "/operations/report-archive",
                "Report",
                REPORT),
            Adjustments.STATUS_EVENT);
      }
      return new JobOutcome(
          companies.size(),
          REPORT + " of " + businessDate + " archived for " + companies.size() + " company(ies)");
    } finally {
      SecurityContextHolder.getContext().setAuthentication(previous);
    }
  }

  /** The job runs the report as the system with the Operations report authorities only. */
  private static Authentication reportAuthority() {
    return new UsernamePasswordAuthenticationToken(
        "SYSTEM",
        null,
        List.of(
            new SimpleGrantedAuthority("OPS_REPORT_VIEW"),
            new SimpleGrantedAuthority("OPS_REPORT_EXPORT")));
  }
}
