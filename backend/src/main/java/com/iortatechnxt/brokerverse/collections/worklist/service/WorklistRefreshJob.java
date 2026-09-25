package com.iortatechnxt.brokerverse.collections.worklist.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistRefreshService.RefreshOutcome;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code CLX_DAILY_REFRESH} (BRCLXN.013-015, 052): after the 22:00 EOD, refreshes the collection
 * worklist of every company from the invoice ledger, ends temporary assignments and assigns new
 * items by rule. A failure raises {@code CLX_REFRESH_FAILED} and fails the run (job monitor, the
 * "EBIX/QPS synchronization view" of p.45-46).
 */
@Component
public class WorklistRefreshJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "CLX_DAILY_REFRESH";

  private final WorklistRefreshService refresh;
  private final CompanyRepository companies;
  private final AlertService alerts;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param refresh refresh
   * @param companies companies
   * @param alerts alerts
   * @param cron schedule ({@code brokerverse.jobs.clx-daily-refresh-cron})
   */
  public WorklistRefreshJob(
      WorklistRefreshService refresh,
      CompanyRepository companies,
      AlertService alerts,
      @Value("${brokerverse.jobs.clx-daily-refresh-cron:-}") String cron) {
    this.refresh = refresh;
    this.companies = companies;
    this.alerts = alerts;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Refreshes the collection worklist from the invoice ledger and assigns new accounts";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int processed = 0;
    List<String> messages = new ArrayList<>();
    for (Company company : companies.findAll()) {
      try {
        RefreshOutcome outcome = refresh.refreshAll(company.getId(), businessDate);
        processed += outcome.created() + outcome.updated() + outcome.closed() + outcome.reopened();
        messages.add(company.getCode() + ": " + outcome.message());
      } catch (RuntimeException ex) {
        alerts.raise(
            "CLX_REFRESH_FAILED",
            new AlertFacts(
                company.getId(),
                null,
                "CollectionWorklist",
                JOB_NAME,
                "The collection worklist refresh of "
                    + businessDate
                    + " failed: "
                    + ex.getMessage(),
                null,
                "CLX_REFRESH_FAILED:" + company.getId() + ":" + businessDate));
        throw new IllegalStateException(
            "Collection worklist refresh of " + company.getCode() + " failed", ex);
      }
    }
    return new JobOutcome(processed, String.join("; ", messages));
  }
}
