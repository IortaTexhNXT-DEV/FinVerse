package com.iortatechnxt.brokerverse.alert.service;

import com.iortatechnxt.brokerverse.alert.service.AlertCheck.AlertSignal;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Daily exception check: evaluates every {@link AlertCheck} and raises the signals found. A failing
 * check is reported in the run message and does not stop the others.
 */
@Component
public class AlertDailyJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "ALERT_DAILY_CHECKS";

  private static final Logger LOG = LoggerFactory.getLogger(AlertDailyJob.class);

  private final List<AlertCheck> checks;
  private final AlertService alerts;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param checks all checks
   * @param alerts alert service
   * @param cron schedule ({@code brokerverse.jobs.alert-checks-cron})
   */
  public AlertDailyJob(
      List<AlertCheck> checks,
      AlertService alerts,
      @Value("${brokerverse.jobs.alert-checks-cron:0 30 1 * * *}") String cron) {
    this.checks = List.copyOf(checks);
    this.alerts = alerts;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Evaluates the exception codes (cash, trial balance, suspense, approval ageing...)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int raised = 0;
    List<String> failures = new ArrayList<>();
    for (AlertCheck check : checks) {
      try {
        for (AlertSignal signal : check.evaluate(businessDate)) {
          raised += alerts.raise(signal.code(), signal.facts()).isPresent() ? 1 : 0;
        }
      } catch (RuntimeException ex) {
        LOG.error("Alert check {} failed", check.getClass().getSimpleName(), ex);
        failures.add(check.getClass().getSimpleName() + ": " + ex.getMessage());
      }
    }
    String message = raised + " new alert(s) from " + checks.size() + " check(s)";
    return new JobOutcome(
        raised,
        failures.isEmpty() ? message : message + "; failed: " + String.join("; ", failures));
  }
}
