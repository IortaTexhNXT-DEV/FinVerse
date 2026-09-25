package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Daily GL-SL reconciliation job ({@code ACSL_GL_SL_RECON}, ACSL 2.13.2; cron {@code
 * brokerverse.jobs.acsl-gl-sl-recon-cron}, default 12:00 UTC = 20:00 PHT): reconciles every control
 * account of every company as of the business date and raises {@code ACSL_GLSL_DIFFERENCE} for the
 * accounts that differ. It also runs at period end when started by hand for that date.
 */
@Component
public class GlSlReconJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "ACSL_GL_SL_RECON";

  private final GlSlReconciliationService reconciliation;
  private final OrganizationService organization;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param reconciliation GL-SL reconciliation
   * @param organization companies
   * @param cron schedule
   */
  public GlSlReconJob(
      GlSlReconciliationService reconciliation,
      OrganizationService organization,
      @Value("${brokerverse.jobs.acsl-gl-sl-recon-cron:-}") String cron) {
    this.reconciliation = reconciliation;
    this.organization = organization;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Reconciles the general ledger with the sub-ledgers per control account (ACSL 2.13.2)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    List<Company> companies = organization.listCompanies();
    int differences = 0;
    int accounts = 0;
    for (Company company : companies) {
      var run = reconciliation.run(company.getId(), businessDate);
      accounts += run.getAccounts();
      differences += run.getDifferences();
    }
    return new JobOutcome(
        accounts,
        accounts
            + " control account(s) of "
            + companies.size()
            + " company(ies) reconciled as of "
            + businessDate
            + ", "
            + differences
            + " with a difference");
  }
}
