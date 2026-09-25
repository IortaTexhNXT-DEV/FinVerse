package com.iortatechnxt.brokerverse.closing.service;

import com.iortatechnxt.brokerverse.closing.domain.PeriodCloseSchedule;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Job {@value PeriodCloseScheduleService#JOB_NAME} (FRBS 2.6.0): runs the scheduled month-end
 * closes whose time has come. Cron {@code brokerverse.jobs.gl-period-close-cron} (default every 15
 * minutes).
 */
@Component
public class GlPeriodCloseJob implements ManagedJob {

  private final PeriodCloseScheduleService service;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param service scheduled closes
   * @param cron schedule, "-" for manual only
   */
  public GlPeriodCloseJob(
      PeriodCloseScheduleService service,
      @Value("${brokerverse.jobs.gl-period-close-cron:-}") String cron) {
    this.service = service;
    this.cron = cron;
  }

  @Override
  public String name() {
    return PeriodCloseScheduleService.JOB_NAME;
  }

  @Override
  public String description() {
    return "Runs the scheduled month-end closes that are due (FRBS 2.6.0)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    List<PeriodCloseSchedule> done = service.runDue();
    long failed =
        done.stream().filter(s -> PeriodCloseSchedule.FAILED.equals(s.getStatus())).count();
    return new JobOutcome(
        done.size(),
        done.size() + " scheduled close(s) run" + (failed == 0 ? "" : ", " + failed + " failed"));
  }
}
