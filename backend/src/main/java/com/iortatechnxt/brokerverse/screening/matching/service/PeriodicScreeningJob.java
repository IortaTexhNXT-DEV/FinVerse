package com.iortatechnxt.brokerverse.screening.matching.service;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code SCR_PERIODIC_SCREENING} (SNSRP-602; cron {@code
 * brokerverse.jobs.scr-periodic-screening-cron}, 01:30 PHT): the batch window. Screens the in-scope
 * clients of every company against the entries changed since the last batch, and the whole list on
 * the full-rescreen day. One run and one transaction per company; a company that fails does not
 * stop the others.
 */
@Component
public class PeriodicScreeningJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "SCR_PERIODIC_SCREENING";

  private static final Logger LOG = LoggerFactory.getLogger(PeriodicScreeningJob.class);

  private final OrganizationService organizations;
  private final BatchScreening batch;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param organizations companies
   * @param batch batch screening
   * @param cron schedule
   */
  public PeriodicScreeningJob(
      OrganizationService organizations,
      BatchScreening batch,
      @Value("${brokerverse.jobs.scr-periodic-screening-cron:-}") String cron) {
    this.organizations = organizations;
    this.batch = batch;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Screens the clients in scope against the changed watchlist entries (monthly: the whole"
        + " list) and logs each run (SNSRP-602)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int runs = 0;
    int clients = 0;
    int matches = 0;
    int failed = 0;
    for (Company company : organizations.listCompanies()) {
      try {
        Optional<ScreeningResult> result = batch.periodic(company.getId(), businessDate, null);
        if (result.isPresent()) {
          runs++;
          clients += result.get().clientsScreened();
          matches += result.get().matches().size();
        }
      } catch (RuntimeException ex) {
        failed++;
        LOG.warn("Periodic screening of company {} failed: {}", company.getCode(), ex.getMessage());
      }
    }
    return new JobOutcome(
        clients,
        runs
            + " screening run(s), "
            + clients
            + " client(s) screened, "
            + matches
            + " new match(es)"
            + (failed > 0 ? ", " + failed + " company run(s) failed" : ""));
  }
}
