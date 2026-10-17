package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Hourly step PLACEMENT_UPDATE_REPORT (BDOI FRS FRPM.007.01; cron {@code
 * brokerverse.jobs.placement-update-cron}): on the configured weekday from the configured time on
 * (Thursday 08:00 by default, business time), generates the Consolidated Placement Update Report of
 * each company once and keeps it in the repository.
 */
@Component
public class PlacementUpdateJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "PLACEMENT_UPDATE_REPORT";

  private final PlacementUpdateService reports;
  private final OrganizationService organization;
  private final Clock clock;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param reports placement update reports
   * @param organization companies
   * @param clock clock
   * @param cron schedule
   */
  public PlacementUpdateJob(
      PlacementUpdateService reports,
      OrganizationService organization,
      Clock clock,
      @Value("${brokerverse.jobs.placement-update-cron:0 0 * * * *}") String cron) {
    this.reports = reports;
    this.organization = organization;
    this.clock = clock;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Generates the weekly Consolidated Placement Update Report on the configured day and time";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    return run(BusinessClock.now(clock));
  }

  /**
   * Generates the reports due at a business date and time.
   *
   * @param now business date and time
   * @return outcome
   */
  public JobOutcome run(ZonedDateTime now) {
    Authentication previous = SecurityContextHolder.getContext().getAuthentication();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "SYSTEM", null, List.of(new SimpleGrantedAuthority("PKG_REPORT_VIEW"))));
    try {
      int done = 0;
      for (Company company : organization.listCompanies()) {
        if (reports.due(company.getId(), now)) {
          reports.generate(company.getId(), now.toLocalDate(), PlacementUpdateService.SCHEDULED);
          done++;
        }
      }
      return new JobOutcome(done, done + " Placement Update Report(s) generated");
    } finally {
      SecurityContextHolder.getContext().setAuthentication(previous);
    }
  }
}
