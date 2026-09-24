package com.iortatechnxt.brokerverse.underwriting.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.system.domain.JobRun;
import com.iortatechnxt.brokerverse.system.domain.JobRunStatus;
import com.iortatechnxt.brokerverse.system.domain.JobTrigger;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.JobRunService;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Daily expiry of lapsed quotations: every open quotation (draft, pending approval or approved) of
 * every active company whose validity ended before the business date becomes EXPIRED. Each company
 * is expired in its own transaction. The Quotations screen starts the same run for one company
 * ({@link #runFor}); both are recorded in the job monitor under {@value #JOB_NAME}.
 */
@Component
public class QuotationExpiryJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "QUOTATION_EXPIRY";

  private final QuotationService quotations;
  private final OrganizationService organization;
  private final JobRunService runs;
  private final Clock clock;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param quotations quotation service
   * @param organization companies
   * @param runs job run recorder (manual runs)
   * @param clock clock (default run date)
   * @param cron schedule ({@code brokerverse.jobs.quotation-expiry-cron})
   */
  public QuotationExpiryJob(
      QuotationService quotations,
      OrganizationService organization,
      JobRunService runs,
      Clock clock,
      @Value("${brokerverse.jobs.quotation-expiry-cron:0 45 0 * * *}") String cron) {
    this.quotations = quotations;
    this.organization = organization;
    this.runs = runs;
    this.clock = clock;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Expires open quotations whose validity has lapsed";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int expired = 0;
    StringBuilder message = new StringBuilder();
    for (Company c : organization.listCompanies()) {
      if (c.isActive()) {
        int count = quotations.expireLapsed(c.getId(), businessDate);
        expired += count;
        message.append(c.getCode()).append(": ").append(count).append("; ");
      }
    }
    return new JobOutcome(
        expired, expired + " quotation(s) expired as of " + businessDate + " (" + message + ")");
  }

  /**
   * Expires the lapsed quotations of one company now (manual run, recorded in the job monitor).
   *
   * @param companyId company
   * @param asOf run date, null for today
   * @return number of quotations expired
   */
  public int runFor(Long companyId, LocalDate asOf) {
    LocalDate date = asOf != null ? asOf : LocalDate.now(clock);
    String company = organization.getCompany(companyId).getCode();
    JobRun run =
        runs.execute(
            JOB_NAME,
            JobTrigger.MANUAL,
            () -> {
              int count = quotations.expireLapsed(companyId, date);
              return new JobOutcome(
                  count, company + ": " + count + " quotation(s) expired as of " + date);
            });
    if (run.getStatus() != JobRunStatus.SUCCEEDED) {
      throw new BusinessRuleException("QUOTATION_EXPIRY_FAILED", run.getMessage());
    }
    return run.getItemsProcessed();
  }
}
