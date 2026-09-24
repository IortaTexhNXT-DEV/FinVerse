package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Applies the payments confirmed by every {@link PaymentConfirmationSource} to the accounts
 * awaiting payment (BRNB.067/068): today the confirmed payment reports, later the Operations
 * Cashiering receipt applications.
 */
@Component
public class PaymentConfirmationSweepJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "PAYMENT_CONFIRMATION_SWEEP";

  private final PaymentConfirmationSweep sweep;
  private final CompanyRepository companies;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param sweep sweep
   * @param companies companies
   * @param cron schedule ({@code brokerverse.jobs.payment-confirmation-sweep-cron})
   */
  public PaymentConfirmationSweepJob(
      PaymentConfirmationSweep sweep,
      CompanyRepository companies,
      @Value("${brokerverse.jobs.payment-confirmation-sweep-cron:0 0 * * * *}") String cron) {
    this.sweep = sweep;
    this.companies = companies;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Opens the payment gate of accounts whose payment was confirmed by a payment source";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int opened = 0;
    for (Company company : companies.findAll()) {
      opened += sweep.sweep(company.getId());
    }
    return new JobOutcome(opened, opened + " account(s) released for placement");
  }
}
