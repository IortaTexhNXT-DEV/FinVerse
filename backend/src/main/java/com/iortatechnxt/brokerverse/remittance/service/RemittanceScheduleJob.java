package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun.Scope;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRunRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTrigger;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceSettingChange.Values;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@code REMITTANCE_SCHEDULE} (FRS BRD-02 FRID.RMT.003.01): every quarter of an hour, runs the
 * scheduled extraction of a company once its extraction time of the Remittance Settings has passed
 * in the Philippines, once per business day, skipping weekends and the head office's holidays when
 * the settings say working days only. Nothing runs while the schedule of the settings is off.
 */
@Component
public class RemittanceScheduleJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "REMITTANCE_SCHEDULE";

  private static final String EVERY_QUARTER_HOUR = "0 */15 * * * *";

  private final RemittanceSettingsService settings;
  private final ExtractionService extraction;
  private final ExtractionRunRepository runs;
  private final OrganizationService organization;
  private final Clock clock;

  /**
   * Creates the job.
   *
   * @param settings schedule of each company
   * @param extraction extraction
   * @param runs runs of the day
   * @param organization companies and working days
   * @param clock clock
   */
  public RemittanceScheduleJob(
      RemittanceSettingsService settings,
      ExtractionService extraction,
      ExtractionRunRepository runs,
      OrganizationService organization,
      Clock clock) {
    this.settings = settings;
    this.extraction = extraction;
    this.runs = runs;
    this.organization = organization;
    this.clock = clock;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Runs the remittance extraction at the time set in the Remittance Settings";
  }

  @Override
  public String cron() {
    return EVERY_QUARTER_HOUR;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    ZonedDateTime now = clock.instant().atZone(BusinessClock.zone());
    List<String> ran = new ArrayList<>();
    for (Company company : organization.listCompanies()) {
      if (due(company.getId(), settings.current(company.getId()), now)) {
        extraction.run(
            company.getId(),
            new Scope(ExtractionTrigger.SCHEDULED, null, null, null),
            now.toLocalDate());
        ran.add(company.getCode());
      }
    }
    return new JobOutcome(
        ran.size(),
        ran.isEmpty() ? "No extraction due" : "Extraction run for " + String.join(", ", ran));
  }

  /**
   * Whether the scheduled extraction of a company is due now.
   *
   * @param companyId company
   * @param values settings in force
   * @param now time in the Philippines
   * @return true when due and not yet run today
   */
  public boolean due(Long companyId, Values values, ZonedDateTime now) {
    if (!values.scheduleActive()
        || now.toLocalTime().isBefore(LocalTime.parse(values.extractionTime()))) {
      return false;
    }
    LocalDate today = now.toLocalDate();
    if (values.workingDaysOnly() && !workingDay(companyId, today)) {
      return false;
    }
    return !runs.existsByCompanyIdAndTriggerAndBusinessDate(
        companyId, ExtractionTrigger.SCHEDULED, today);
  }

  private boolean workingDay(Long companyId, LocalDate date) {
    return organization.listBranches(companyId).stream()
        .filter(Branch::isHeadOffice)
        .findFirst()
        .map(b -> organization.isWorkingDay(b, date))
        .orElse(date.getDayOfWeek().getValue() < java.time.DayOfWeek.SATURDAY.getValue());
  }
}
