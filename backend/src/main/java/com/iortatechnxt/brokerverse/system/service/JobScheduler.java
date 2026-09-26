package com.iortatechnxt.brokerverse.system.service;

import com.iortatechnxt.brokerverse.common.runtime.ConditionalOnWorkload;
import com.iortatechnxt.brokerverse.common.runtime.CurrentRuntimeRole;
import com.iortatechnxt.brokerverse.common.runtime.Workload;
import com.iortatechnxt.brokerverse.system.domain.JobTrigger;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/**
 * Schedules every {@link ManagedJob} by its cron expression (UTC) through the job registry, when
 * the runtime role of the instance runs the job's {@link ManagedJob#workload() workload}: batch
 * jobs on {@code jobs} instances, integration jobs on {@code integration} instances, both on {@code
 * all}. Exists only on the roles that schedule anything; a {@code web} instance schedules nothing.
 */
@Component
@ConditionalOnWorkload({Workload.BATCH, Workload.INTEGRATION})
public class JobScheduler implements SchedulingConfigurer {

  private static final Logger LOG = LoggerFactory.getLogger(JobScheduler.class);

  private final JobRegistry registry;
  private final CurrentRuntimeRole role;

  /**
   * Creates the scheduler.
   *
   * @param registry job registry
   * @param role runtime role of this instance
   */
  public JobScheduler(JobRegistry registry, CurrentRuntimeRole role) {
    this.registry = registry;
    this.role = role;
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar registrar) {
    TimeZone utc = TimeZone.getTimeZone(ZoneOffset.UTC);
    for (ManagedJob job : scheduledJobs()) {
      registrar.addTriggerTask(
          new TriggerTask(
              () -> registry.run(job.name(), JobTrigger.SCHEDULED),
              new CronTrigger(job.cron(), utc)));
    }
  }

  /**
   * The jobs this instance schedules: a schedule is set and the role runs the job's workload.
   *
   * @return jobs ordered by name
   */
  public List<ManagedJob> scheduledJobs() {
    List<ManagedJob> scheduled = new ArrayList<>();
    for (ManagedJob job : registry.all()) {
      if (!JobRegistry.DISABLED.equals(job.cron()) && role.runs(job.workload())) {
        scheduled.add(job);
      }
    }
    LOG.info(
        "Runtime role {} schedules {} job(s): {}",
        role.role(),
        scheduled.size(),
        scheduled.stream().map(ManagedJob::name).toList());
    return scheduled;
  }
}
