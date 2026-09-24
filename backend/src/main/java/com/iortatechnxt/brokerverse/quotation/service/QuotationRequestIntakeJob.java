package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Stores the quotation requests of the source systems (BRNB.023/028) in the request staging table.
 * Manual only by default: no source system is connected until the HLS interface is specified (Q11).
 */
@Component
public class QuotationRequestIntakeJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "QUOTATION_REQUEST_INTAKE";

  private final QuotationRequestService requests;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param requests request service
   * @param cron schedule ({@code brokerverse.jobs.quotation-request-intake-cron}, "-" = manual)
   */
  public QuotationRequestIntakeJob(
      QuotationRequestService requests,
      @Value("${brokerverse.jobs.quotation-request-intake-cron:-}") String cron) {
    this.requests = requests;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Stores the quotation requests received from source systems (HLS) in the request inbox";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int received = requests.pullFromSources();
    return new JobOutcome(received, received + " quotation request(s) received");
  }
}
