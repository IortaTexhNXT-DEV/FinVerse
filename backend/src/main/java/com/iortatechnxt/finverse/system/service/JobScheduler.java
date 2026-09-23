package com.iortatechnxt.finverse.system.service;

import com.iortatechnxt.finverse.system.domain.JobTrigger;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/** Schedules every {@link ManagedJob} by its cron expression (UTC) through the job registry. */
@Component
public class JobScheduler implements SchedulingConfigurer {

  private final JobRegistry registry;

  /**
   * Creates the scheduler.
   *
   * @param registry job registry
   */
  public JobScheduler(JobRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar registrar) {
    TimeZone utc = TimeZone.getTimeZone(ZoneOffset.UTC);
    for (ManagedJob job : registry.all()) {
      if (!JobRegistry.DISABLED.equals(job.cron())) {
        registrar.addTriggerTask(
            new TriggerTask(
                () -> registry.run(job.name(), JobTrigger.SCHEDULED),
                new CronTrigger(job.cron(), utc)));
      }
    }
  }
}
