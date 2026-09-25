package com.iortatechnxt.brokerverse.collections.files.service;

import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile.Status;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The jobs publishing the Collections files for every company (BRCLXN.024-029, 045); crons in
 * {@code brokerverse.jobs.clx-*-files-cron}. A file that fails is recorded and alerted by {@link
 * ScheduledFileService}; the run reports the counts.
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class CollectionFileJobs {

  private CollectionFileJobs() {}

  /** Shared run of a file job over every company. */
  abstract static class FileJob implements ManagedJob {

    private final CompanyRepository companies;
    private final String cron;

    FileJob(CompanyRepository companies, String cron) {
      this.companies = companies;
      this.cron = cron;
    }

    @Override
    public String cron() {
      return cron;
    }

    JobOutcome publishAll(
        LocalDate day, BiFunction<Long, LocalDate, List<ScheduledFile>> publication) {
      List<ScheduledFile> all = new ArrayList<>();
      for (Company company : companies.findAll()) {
        all.addAll(publication.apply(company.getId(), day));
      }
      long failed = all.stream().filter(f -> f.getStatus() == Status.FAILED).count();
      return new JobOutcome(
          all.size(), all.size() + " file(s) for " + day + ", " + failed + " failed");
    }
  }

  /** {@code CLX_DAILY_FILES}: Outstanding PR List and Full Production Report (BRCLXN.045). */
  @Component
  public static class DailyFilesJob extends FileJob {

    private final CollectionFiles files;

    /**
     * Creates the job.
     *
     * @param files Collections files
     * @param companies companies
     * @param cron schedule ({@code brokerverse.jobs.clx-daily-files-cron})
     */
    public DailyFilesJob(
        CollectionFiles files,
        CompanyRepository companies,
        @Value("${brokerverse.jobs.clx-daily-files-cron:-}") String cron) {
      super(companies, cron);
      this.files = files;
    }

    @Override
    public String name() {
      return "CLX_DAILY_FILES";
    }

    @Override
    public String description() {
      return "Publishes the daily Outstanding PR List and Full Production Report";
    }

    @Override
    public JobOutcome execute(LocalDate businessDate) {
      return publishAll(businessDate, files::daily);
    }
  }

  /** {@code CLX_WEEKLY_FILES}: DP PR / PR 2307 for reversal per unit and branch (028/029). */
  @Component
  public static class WeeklyFilesJob extends FileJob {

    private final CollectionFiles files;

    /**
     * Creates the job.
     *
     * @param files Collections files
     * @param companies companies
     * @param cron schedule ({@code brokerverse.jobs.clx-weekly-files-cron})
     */
    public WeeklyFilesJob(
        CollectionFiles files,
        CompanyRepository companies,
        @Value("${brokerverse.jobs.clx-weekly-files-cron:-}") String cron) {
      super(companies, cron);
      this.files = files;
    }

    @Override
    public String name() {
      return "CLX_WEEKLY_FILES";
    }

    @Override
    public String description() {
      return "Publishes the weekly DP PR and PR 2307 for reversal files, available Monday 08:00";
    }

    @Override
    public JobOutcome execute(LocalDate businessDate) {
      return publishAll(businessDate, files::weekly);
    }
  }

  /**
   * {@code CLX_MONTHLY_FILES}: runs daily at 05:00 Philippine time and publishes the previous
   * month's DP PR / PR 2307 for reversal on the first working day (BRCLXN.024-027).
   */
  @Component
  public static class MonthlyFilesJob extends FileJob {

    private final CollectionFiles files;
    private final Clock clock;

    /**
     * Creates the job.
     *
     * @param files Collections files
     * @param companies companies
     * @param clock clock (the run is at 21:00 UTC, the next day in Manila)
     * @param cron schedule ({@code brokerverse.jobs.clx-monthly-files-cron})
     */
    public MonthlyFilesJob(
        CollectionFiles files,
        CompanyRepository companies,
        Clock clock,
        @Value("${brokerverse.jobs.clx-monthly-files-cron:-}") String cron) {
      super(companies, cron);
      this.files = files;
      this.clock = clock;
    }

    @Override
    public String name() {
      return "CLX_MONTHLY_FILES";
    }

    @Override
    public String description() {
      return "Publishes the monthly DP PR and PR 2307 for reversal files on the first working day";
    }

    @Override
    public JobOutcome execute(LocalDate businessDate) {
      LocalDate manilaDay = clock.instant().atZone(FilePeriods.MANILA).toLocalDate();
      return publishAll(manilaDay, files::monthly);
    }
  }
}
