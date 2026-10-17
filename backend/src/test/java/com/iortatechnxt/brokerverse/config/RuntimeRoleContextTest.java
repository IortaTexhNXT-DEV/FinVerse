package com.iortatechnxt.brokerverse.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.common.runtime.CurrentRuntimeRole;
import com.iortatechnxt.brokerverse.common.runtime.Workload;
import com.iortatechnxt.brokerverse.events.service.DeadLetterRecorder;
import com.iortatechnxt.brokerverse.events.service.DeadLetterStore;
import com.iortatechnxt.brokerverse.events.service.EventArchiveConsumer;
import com.iortatechnxt.brokerverse.events.service.EventArchiveStore;
import com.iortatechnxt.brokerverse.events.service.EventEnvelopeReader;
import com.iortatechnxt.brokerverse.integration.service.NotificationDeliveryConsumer;
import com.iortatechnxt.brokerverse.messaging.service.MailDispatcher;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.JobRegistry;
import com.iortatechnxt.brokerverse.system.service.JobScheduler;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.ScheduledTask;

/**
 * Which schedulers, scheduled jobs and Kafka consumers each runtime role starts: the real
 * scheduling configuration, job scheduler and consumer beans with their collaborators mocked.
 */
class RuntimeRoleContextTest {

  private static final String NEVER = "0 0 0 1 1 *";

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withPropertyValues("brokerverse.kafka.enabled=true")
          .withUserConfiguration(Collaborators.class)
          .withBean(CurrentRuntimeRole.class)
          .withUserConfiguration(SchedulingConfiguration.class)
          .withBean(JobScheduler.class)
          .withBean(EventArchiveConsumer.class)
          .withBean(DeadLetterRecorder.class)
          .withBean(NotificationDeliveryConsumer.class);

  @Test
  void webRunsNoSchedulerNoJobAndNoConsumer() {
    runner
        .withPropertyValues("brokerverse.runtime.role=web")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
              assertThat(context).doesNotHaveBean(JobScheduler.class);
              assertThat(context).doesNotHaveBean(EventArchiveConsumer.class);
              assertThat(context).doesNotHaveBean(DeadLetterRecorder.class);
              assertThat(context).doesNotHaveBean(NotificationDeliveryConsumer.class);
            });
  }

  @Test
  void jobsSchedulesTheBatchJobsOnlyAndRunsNoConsumer() {
    runner
        .withPropertyValues("brokerverse.runtime.role=jobs")
        .run(
            context -> {
              assertThat(scheduled(context.getBean(ScheduledAnnotationBeanPostProcessor.class)))
                  .isEqualTo(1);
              assertThat(context.getBean(JobScheduler.class).scheduledJobs())
                  .extracting(ManagedJob::name)
                  .containsExactly("BATCH_JOB");
              assertThat(context).doesNotHaveBean(EventArchiveConsumer.class);
              assertThat(context).doesNotHaveBean(DeadLetterRecorder.class);
              assertThat(context).doesNotHaveBean(NotificationDeliveryConsumer.class);
            });
  }

  @Test
  void integrationSchedulesTheIntegrationJobsAndRunsTheConsumers() {
    runner
        .withPropertyValues("brokerverse.runtime.role=integration")
        .run(
            context -> {
              assertThat(scheduled(context.getBean(ScheduledAnnotationBeanPostProcessor.class)))
                  .isEqualTo(1);
              assertThat(context.getBean(JobScheduler.class).scheduledJobs())
                  .extracting(ManagedJob::name)
                  .containsExactly("INTEGRATION_JOB");
              assertThat(context).hasSingleBean(EventArchiveConsumer.class);
              assertThat(context).hasSingleBean(DeadLetterRecorder.class);
              assertThat(context).hasSingleBean(NotificationDeliveryConsumer.class);
            });
  }

  @Test
  void allRunsEverythingAndIsTheDefault() {
    runner.run(
        context -> {
          assertThat(context.getBean(CurrentRuntimeRole.class).runs(Workload.USER_API)).isTrue();
          assertThat(scheduled(context.getBean(ScheduledAnnotationBeanPostProcessor.class)))
              .isEqualTo(2);
          assertThat(context.getBean(JobScheduler.class).scheduledJobs())
              .extracting(ManagedJob::name)
              .containsExactly("BATCH_JOB", "INTEGRATION_JOB");
          assertThat(context).hasSingleBean(EventArchiveConsumer.class);
          assertThat(context).hasSingleBean(NotificationDeliveryConsumer.class);
        });
  }

  @Test
  void consumersStayOffWithKafkaDisabledWhateverTheRole() {
    runner
        .withPropertyValues(
            "brokerverse.runtime.role=integration", "brokerverse.kafka.enabled=false")
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(EventArchiveConsumer.class);
              assertThat(context).doesNotHaveBean(NotificationDeliveryConsumer.class);
              assertThat(context).hasSingleBean(JobScheduler.class);
            });
  }

  @Test
  void anUnknownRoleStopsTheStart() {
    runner
        .withPropertyValues("brokerverse.runtime.role=batch")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasStackTraceContaining("web, jobs, integration or all");
            });
  }

  private static int scheduled(ScheduledAnnotationBeanPostProcessor processor) {
    int count = 0;
    for (ScheduledTask ignored : processor.getScheduledTasks()) {
      count++;
    }
    return count;
  }

  /** Mocked collaborators and three jobs: one batch, one integration, one without schedule. */
  @Configuration(proxyBeanMethods = false)
  static class Collaborators {

    @Bean
    JobRegistry jobRegistry() {
      JobRegistry registry = mock(JobRegistry.class);
      when(registry.all())
          .thenReturn(
              List.of(
                  new TestJob("BATCH_JOB", NEVER, Workload.BATCH),
                  new TestJob("INTEGRATION_JOB", NEVER, Workload.INTEGRATION),
                  new TestJob("MANUAL_JOB", JobRegistry.DISABLED, Workload.BATCH)));
      return registry;
    }

    @Bean
    Clock clock() {
      return Clock.systemUTC();
    }

    @Bean
    EventEnvelopeReader eventEnvelopeReader() {
      return mock(EventEnvelopeReader.class);
    }

    @Bean
    EventArchiveStore eventArchiveStore() {
      return mock(EventArchiveStore.class);
    }

    @Bean
    DeadLetterStore deadLetterStore() {
      return mock(DeadLetterStore.class);
    }

    @Bean
    MailDispatcher mailDispatcher() {
      return mock(MailDispatcher.class);
    }
  }

  private record TestJob(String name, String cron, Workload workload) implements ManagedJob {

    @Override
    public String description() {
      return name;
    }

    @Override
    public JobOutcome execute(LocalDate businessDate) {
      return new JobOutcome(0, name);
    }
  }
}
