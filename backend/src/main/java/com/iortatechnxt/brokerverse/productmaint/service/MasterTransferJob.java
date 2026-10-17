package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Scheduled transfer PRODUCT_MASTER_TRANSFER of the product master changes to the other BDOI
 * systems (BDOI FRS FRPM.029.01; cron {@code brokerverse.jobs.product-master-cron}, every 30
 * minutes by default).
 */
@Component
public class MasterTransferJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "PRODUCT_MASTER_TRANSFER";

  private final MasterTransfer transfer;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param transfer the transfer
   * @param cron schedule
   */
  public MasterTransferJob(
      MasterTransfer transfer,
      @Value("${brokerverse.jobs.product-master-cron:0 */30 * * * *}") String cron) {
    this.transfer = transfer;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Sends the product master changes to the other systems of the bank";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    MasterTransfer.TransferRun run = transfer.transfer();
    return new JobOutcome(
        run.sent(),
        run.sent()
            + " change(s) sent"
            + (run.fileName() == null ? "" : " in " + run.fileName())
            + "; "
            + run.waiting()
            + " waiting; target "
            + run.target());
  }
}
