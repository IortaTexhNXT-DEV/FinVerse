package com.iortatechnxt.brokerverse.screening.watchlist.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code SCR_INGEST_ERROR_DIGEST} (SNSRP-202; cron {@code
 * brokerverse.jobs.scr-ingest-error-digest-cron}, 07:00 PHT Monday to Friday): e-mails the records
 * that failed ingestion since the last digest to {@code SCR_INGEST_ALERT_RECIPIENTS}.
 */
@Component
public class IngestErrorDigestJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "SCR_INGEST_ERROR_DIGEST";

  private final IngestErrorDigest digest;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param digest the digest
   * @param cron schedule
   */
  public IngestErrorDigestJob(
      IngestErrorDigest digest,
      @Value("${brokerverse.jobs.scr-ingest-error-digest-cron:-}") String cron) {
    this.digest = digest;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "E-mails the records that failed watchlist ingestion to the recipients (SNSRP-202)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    return digest.send();
  }
}
