package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code BCL_AGEING_ALERTS} (BRCLM.031, p.43, FR-CL-061 R2; cron {@code
 * brokerverse.jobs.bcl-ageing-alerts-cron}, 06:00 Manila): raises {@code BCL_CLAIM_PAST_DUE} for
 * every outstanding claim (temporarily closed included, CLQ06) older than {@code BCL_PAST_DUE_DAYS}
 * from its reported date, once per claim, and tells the handler.
 */
@Component
public class AgeingAlertsJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "BCL_AGEING_ALERTS";

  private static final int DEFAULT_PAST_DUE_DAYS = 90;

  private final ClaimReminders reminders;
  private final SystemParameterService parameters;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param reminders claim reminders
   * @param parameters business parameters
   * @param cron schedule ({@code brokerverse.jobs.bcl-ageing-alerts-cron})
   */
  public AgeingAlertsJob(
      ClaimReminders reminders,
      SystemParameterService parameters,
      @Value("${brokerverse.jobs.bcl-ageing-alerts-cron:-}") String cron) {
    this.reminders = reminders;
    this.parameters = parameters;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Flags outstanding claims older than BCL_PAST_DUE_DAYS (BRCLM.031)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int days = parameters.intValue(ClaimCodes.PARAM_PAST_DUE_DAYS, DEFAULT_PAST_DUE_DAYS);
    List<Map<String, Object>> claims = reminders.pastDue(businessDate, days);
    int raised = 0;
    for (Map<String, Object> row : claims) {
      if (reminders.flagPastDue(row, days)) {
        raised++;
      }
    }
    return new JobOutcome(
        claims.size(), claims.size() + " claim(s) past due, " + raised + " new alert(s)");
  }
}
