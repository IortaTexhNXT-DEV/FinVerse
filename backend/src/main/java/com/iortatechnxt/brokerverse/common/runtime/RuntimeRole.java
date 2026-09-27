package com.iortatechnxt.brokerverse.common.runtime;

import java.util.EnumSet;
import java.util.Set;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

/**
 * Workload role of one instance, {@code brokerverse.runtime.role} ({@code
 * BROKERVERSE_RUNTIME_ROLE}). The same image runs as the {@code bibs-web}, {@code bibs-jobs} and
 * {@code bibs-integration} deployments, each with its own role (ARCHITECTURE_OPTION_DECISION.md
 * section 2); {@link #ALL} runs everything in one process (local development, tests, seed stacks).
 */
public enum RuntimeRole {
  /** User screens and APIs; no schedulers, no Kafka consumers, no outbox relay. */
  WEB(EnumSet.of(Workload.USER_API)),
  /** Scheduled and batch jobs; HTTP limited to the actuator (health probes, metrics). */
  JOBS(EnumSet.of(Workload.BATCH)),
  /** Outbox relay, Kafka consumers, inbound files and the {@code /integration/**} APIs. */
  INTEGRATION(EnumSet.of(Workload.INTEGRATION)),
  /** Every workload in one process. */
  ALL(EnumSet.allOf(Workload.class));

  /** Property naming the role. */
  public static final String PROPERTY = "brokerverse.runtime.role";

  private final Set<Workload> workloads;

  RuntimeRole(Set<Workload> workloads) {
    this.workloads = workloads;
  }

  /**
   * Tells whether this role carries a workload.
   *
   * @param workload workload
   * @return true when instances of this role run it
   */
  public boolean runs(Workload workload) {
    return workloads.contains(workload);
  }

  /**
   * Tells whether this role runs scheduled work (batch jobs or the integration jobs).
   *
   * @return true when a task scheduler is needed
   */
  public boolean schedules() {
    return runs(Workload.BATCH) || runs(Workload.INTEGRATION);
  }

  /**
   * Resolves the role of an environment (relaxed, case-insensitive binding); {@link #ALL} when the
   * property is not set.
   *
   * @param environment environment
   * @return role
   * @throws IllegalStateException when the value is not a role
   */
  public static RuntimeRole of(Environment environment) {
    try {
      return Binder.get(environment).bind(PROPERTY, RuntimeRole.class).orElse(ALL);
    } catch (RuntimeException ex) {
      throw new IllegalStateException(
          PROPERTY + " must be one of web, jobs, integration or all", ex);
    }
  }
}
