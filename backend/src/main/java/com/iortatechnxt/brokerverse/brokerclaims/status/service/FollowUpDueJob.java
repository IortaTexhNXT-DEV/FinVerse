package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code BCL_FOLLOW_UP_DUE} (BRCLM.019/022/034, FR-CL-054; cron {@code
 * brokerverse.jobs.bcl-follow-up-due-cron}, 06:00 Manila): notifies each handler of the claims
 * whose next follow-up date is today and each assignee of the diary entries due today ({@code
 * BCL_FOLLOW_UP_DUE}), and raises {@code BCL_FOLLOW_UP_OVERDUE} once per claim for past dates. Each
 * item runs in its own transaction.
 */
@Component
public class FollowUpDueJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "BCL_FOLLOW_UP_DUE";

  private final ClaimReminders reminders;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param reminders claim reminders
   * @param cron schedule ({@code brokerverse.jobs.bcl-follow-up-due-cron})
   */
  public FollowUpDueJob(
      ClaimReminders reminders,
      @Value("${brokerverse.jobs.bcl-follow-up-due-cron:-}") String cron) {
    this.reminders = reminders;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Reminds claims handlers of follow-ups and diary entries due; flags overdue ones"
        + " (BRCLM.019/022/034)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    List<Map<String, Object>> due = reminders.followUps(businessDate);
    int acted = 0;
    for (Map<String, Object> row : due) {
      if (reminders.remind(row, businessDate)) {
        acted++;
      }
    }
    return new JobOutcome(
        due.size(), due.size() + " follow-up(s) and diary entries due, " + acted + " reminded");
  }
}
