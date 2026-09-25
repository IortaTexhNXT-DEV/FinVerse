package com.iortatechnxt.brokerverse.collections.unapplied.service;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code CLX_APPLICATION_FILE} (BRCLXN.041/042): at 05:00 Philippine time, before the 06:00
 * deadline, writes for every company the "For Application To Invoice" text file of the requests of
 * the previous day (cron {@code brokerverse.jobs.clx-application-file-cron}).
 */
@Component
public class ApplicationFileJob implements ManagedJob {

  private final ApplicationFileService files;
  private final CompanyRepository companies;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param files application file
   * @param companies companies
   * @param cron schedule
   */
  public ApplicationFileJob(
      ApplicationFileService files,
      CompanyRepository companies,
      @Value("${brokerverse.jobs.clx-application-file-cron:-}") String cron) {
    this.files = files;
    this.companies = companies;
    this.cron = cron;
  }

  @Override
  public String name() {
    return "CLX_APPLICATION_FILE";
  }

  @Override
  public String description() {
    return "Writes the daily For Application To Invoice text file to FS04 before 06:00";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    LocalDate day = files.previousDay();
    int written = 0;
    for (Company company : companies.findAll()) {
      if (files.publish(company.getId(), day).isPresent()) {
        written++;
      }
    }
    return new JobOutcome(written, written + " file(s) for the requests of " + day);
  }
}
