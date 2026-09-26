package com.iortatechnxt.brokerverse.events.service;

import com.iortatechnxt.brokerverse.common.runtime.Workload;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Safety net of the outbox relay: sends what the after-commit relay left (broker down, instance
 * stopped) and the retries that are due. With Kafka disabled it marks leftover rows LOCAL.
 */
@Component
public class OutboxRelayJob implements ManagedJob {

  private final OutboxRelay relay;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param relay relay
   * @param cron schedule ({@code brokerverse.jobs.event-outbox-relay-cron}, every minute)
   */
  public OutboxRelayJob(
      OutboxRelay relay,
      @Value("${brokerverse.jobs.event-outbox-relay-cron:0 * * * * *}") String cron) {
    this.relay = relay;
    this.cron = cron;
  }

  @Override
  public String name() {
    return OutboxRelay.JOB_NAME;
  }

  @Override
  public String description() {
    return "Sends pending integration events of the outbox to Kafka and retries failed sends";
  }

  /**
   * Relays the outbox to Kafka: runs on the integration deployment.
   *
   * @return {@link Workload#INTEGRATION}
   */
  @Override
  public Workload workload() {
    return Workload.INTEGRATION;
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int sent = relay.drainUnderJob();
    return new JobOutcome(sent, sent + " integration event(s) relayed");
  }
}
