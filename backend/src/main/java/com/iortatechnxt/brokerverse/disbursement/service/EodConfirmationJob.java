package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.EodStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.EodRun;
import com.iortatechnxt.brokerverse.disbursement.domain.EodRunRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * {@code DISB_EOD_CONFIRMATION} (DIS 2.7.12, cron {@code
 * brokerverse.jobs.disb-eod-confirmation-cron}, "-" = manual): e-mails the payment confirmations of
 * the end-of-day runs up to the business date that are not yet confirmed.
 */
@Component
public class EodConfirmationJob extends EodRunJob {

  /** Job name. */
  public static final String JOB_NAME = "DISB_EOD_CONFIRMATION";

  private final EodConfirmations confirmations;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param runs EOD runs
   * @param confirmations confirmations
   * @param txManager transactions
   * @param cron schedule
   */
  public EodConfirmationJob(
      EodRunRepository runs,
      EodConfirmations confirmations,
      PlatformTransactionManager txManager,
      @Value("${brokerverse.jobs.disb-eod-confirmation-cron:-}") String cron) {
    super(runs, txManager);
    this.confirmations = confirmations;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "E-mails the payment confirmations of the Disbursement end-of-day runs";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  protected List<EodRun> select(LocalDate businessDate) {
    return runs.findByStatusAndBusinessDateLessThanEqualOrderByIdAsc(
        EodStatus.COMPLETED, businessDate);
  }

  @Override
  protected int process(EodRun run) {
    return confirmations.confirm(run);
  }
}
