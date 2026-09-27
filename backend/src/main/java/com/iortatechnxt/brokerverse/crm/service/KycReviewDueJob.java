package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.crm.service.KycReviewService.KycReviewRun;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Monthly KYC review job (BRNB.110): expires overdue KYC, produces the list of clients due for
 * review and notifies the Account Officers.
 */
@Component
public class KycReviewDueJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "KYC_REVIEW_DUE";

  private final KycReviewService reviews;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param reviews KYC review service
   * @param cron schedule ({@code brokerverse.jobs.kyc-review-due-cron})
   */
  public KycReviewDueJob(
      KycReviewService reviews,
      @Value("${brokerverse.jobs.kyc-review-due-cron:0 0 2 1 * *}") String cron) {
    this.reviews = reviews;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Expires overdue client KYC and notifies Account Officers of the KYC reviews due";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    KycReviewRun run = reviews.runReview(businessDate);
    return new JobOutcome(
        run.expired(),
        run.expired()
            + " KYC expired; "
            + run.due()
            + " non-bank client(s) due for review; "
            + run.notified()
            + " user(s) notified");
  }
}
