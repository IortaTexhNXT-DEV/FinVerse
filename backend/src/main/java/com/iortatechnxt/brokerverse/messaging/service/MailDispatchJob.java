package com.iortatechnxt.brokerverse.messaging.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Delivers queued e-mails and retries failed attempts (outbox relay). */
@Component
public class MailDispatchJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "MAIL_DISPATCH";

  private static final int BATCH = 500;

  private final MailDispatcher dispatcher;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param dispatcher dispatcher
   * @param cron schedule ({@code brokerverse.jobs.mail-dispatch-cron})
   */
  public MailDispatchJob(
      MailDispatcher dispatcher,
      @Value("${brokerverse.jobs.mail-dispatch-cron:0 */2 * * * *}") String cron) {
    this.dispatcher = dispatcher;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Delivers queued e-mails (quotations, slips, e-policies, invoices) and retries failures";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int attempted = dispatcher.dispatchPending(BATCH);
    return new JobOutcome(attempted, attempted + " e-mail(s) attempted");
  }
}
