package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.Trigger;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code OPS_INVOICE_FEED_REPLAY}: copies into the Operations ledger every booked invoice that is
 * missing from it (OPERATIONS_DESIGN section 10). Manual by default; also run at seed start-up.
 */
@Component
public class InvoiceFeedReplayJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "OPS_INVOICE_FEED_REPLAY";

  private final InvoiceFeedReplayService replay;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param replay replay service
   * @param cron schedule ({@code brokerverse.jobs.ops-invoice-feed-replay-cron}, "-" = manual)
   */
  public InvoiceFeedReplayJob(
      InvoiceFeedReplayService replay,
      @Value("${brokerverse.jobs.ops-invoice-feed-replay-cron:-}") String cron) {
    this.replay = replay;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Copies booked invoices missing from the Operations invoice ledger";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    FlowInRun run = replay.replayAll(Trigger.SCHEDULED);
    return new JobOutcome(run.getReadCount(), run.getRunNo() + ": " + run.getMessage());
  }
}
