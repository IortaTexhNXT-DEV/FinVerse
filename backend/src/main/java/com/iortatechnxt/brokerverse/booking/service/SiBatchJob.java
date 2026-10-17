package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.SiBatch;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Daily job generating and sending the service-invoice batch of each insurer (G09). */
@Component
public class SiBatchJob implements ManagedJob {

  private final SiBatchService service;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param service batches
   * @param cron schedule ({@code brokerverse.jobs.si-daily-batch-cron})
   */
  public SiBatchJob(
      SiBatchService service,
      @Value("${brokerverse.jobs.si-daily-batch-cron:0 30 13 * * *}") String cron) {
    this.service = service;
    this.cron = cron;
  }

  @Override
  public String name() {
    return SiBatchService.JOB_NAME;
  }

  @Override
  public String description() {
    return "Generates and e-mails the daily service-invoice batch of each insurer";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    List<SiBatch> batches = service.generate(businessDate);
    int invoices = batches.stream().mapToInt(SiBatch::getItemCount).sum();
    return new JobOutcome(
        batches.size(),
        batches.size() + " batches with " + invoices + " service invoices for " + businessDate);
  }
}
