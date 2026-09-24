package com.iortatechnxt.brokerverse.reserves.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Monthly job preparing the valuation run of the previous month for the companies listed in the
 * system parameter {@code RESERVES_AUTO_RUN_COMPANIES}, and submitting it for approval (approval
 * and posting stay manual). Companies that already have a run for the month are skipped. Disabled
 * by default: set {@code brokerverse.jobs.reserve-valuation-cron} (e.g. {@code 0 0 2 1 * *}) to
 * schedule it; administrators can always start it with "Run now".
 */
@Component
public class ReserveValuationJob implements ManagedJob {

  /** Job name. */
  public static final String NAME = "RESERVE_VALUATION";

  /** System parameter listing the company codes to value. */
  public static final String COMPANIES = "RESERVES_AUTO_RUN_COMPANIES";

  private final ValuationRunService runs;
  private final OrganizationService organization;
  private final SystemParameterService parameters;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param runs valuation runs
   * @param organization companies
   * @param parameters system parameters
   * @param cron schedule ({@code brokerverse.jobs.reserve-valuation-cron}, "-" = manual only)
   */
  public ReserveValuationJob(
      ValuationRunService runs,
      OrganizationService organization,
      SystemParameterService parameters,
      @Value("${brokerverse.jobs.reserve-valuation-cron:-}") String cron) {
    this.runs = runs;
    this.organization = organization;
    this.parameters = parameters;
    this.cron = cron;
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String description() {
    return "Prepares and submits the actuarial reserve valuation run of the previous month";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    LocalDate valuationDate = ValuationRunService.monthEnd(businessDate.minusMonths(1));
    List<String> codes = parameters.items(COMPANIES);
    List<String> done = new ArrayList<>();
    for (Company company : organization.listCompanies()) {
      if (codes.contains(company.getCode()) && prepare(company.getId(), valuationDate)) {
        done.add(company.getCode());
      }
    }
    return new JobOutcome(
        done.size(),
        done.isEmpty()
            ? "No valuation run prepared for " + valuationDate
            : "Valuation runs " + valuationDate + " submitted for " + String.join(", ", done));
  }

  private boolean prepare(Long companyId, LocalDate valuationDate) {
    if (runs.forMonth(companyId, valuationDate).isPresent()) {
      return false;
    }
    try {
      runs.submit(runs.create(companyId, valuationDate).getId());
      return true;
    } catch (BusinessRuleException ex) {
      return false;
    }
  }
}
