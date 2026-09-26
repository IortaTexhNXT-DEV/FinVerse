package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.nbadmin.service.RetentionService.ReviewResult;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Monthly data retention review (BRNB.106): counts the records eligible under each retention rule
 * and records the result. It never archives or deletes (parked).
 */
@Component
public class RetentionReviewJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "RETENTION_REVIEW";

  private final RetentionService retention;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param retention retention service
   * @param cron schedule ({@code brokerverse.jobs.retention-review-cron})
   */
  public RetentionReviewJob(
      RetentionService retention,
      @Value("${brokerverse.jobs.retention-review-cron:0 0 3 2 * *}") String cron) {
    this.retention = retention;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Counts records eligible for archiving under the data retention rules (no deletion)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    ReviewResult result = retention.review(businessDate);
    return new JobOutcome(
        result.rulesEvaluated(),
        result.rulesEvaluated()
            + " rule(s) evaluated; "
            + result.eligibleRecords()
            + " record(s) eligible");
  }
}
