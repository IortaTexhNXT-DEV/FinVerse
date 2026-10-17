package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.disbursement.domain.EodRun;
import com.iortatechnxt.brokerverse.disbursement.domain.EodRunRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * {@code DISB_EOD_REPORTS} (DIS 3.28.0, cron {@code brokerverse.jobs.disb-eod-reports-cron}, "-" =
 * manual): produces the end-of-day reports that a run up to the business date misses (the EOD run
 * produces them at once; the job catches up after a failure or a new report).
 */
@Component
public class EodReportsJob extends EodRunJob {

  /** Job name. */
  public static final String JOB_NAME = "DISB_EOD_REPORTS";

  private final EodReports reports;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param runs EOD runs
   * @param reports EOD reports
   * @param txManager transactions
   * @param cron schedule
   */
  public EodReportsJob(
      EodRunRepository runs,
      EodReports reports,
      PlatformTransactionManager txManager,
      @Value("${brokerverse.jobs.disb-eod-reports-cron:-}") String cron) {
    super(runs, txManager);
    this.reports = reports;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Produces the Disbursement end-of-day reports of the runs that miss one";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  protected List<EodRun> select(LocalDate businessDate) {
    return runs.findByBusinessDateLessThanEqualOrderByIdAsc(businessDate);
  }

  @Override
  protected int process(EodRun run) {
    return reports.produce(run);
  }
}
