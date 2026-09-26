package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Job {@code USER_SESSION_SWEEP} (UAM-NFR-35; FR-UA-002, FR-UA-004): ends the sessions nobody signs
 * out of - the browser closed (IDLE_TIMEOUT after {@code SESSION_TIMEOUT_MINUTES} without a
 * request, plus the five-minute activity granularity), the token expired (EXPIRED), the user locked
 * (LOCKED) or disabled (ADMIN_ENDED) - so the session list and the "Online" status stay true. Every
 * 15 minutes by default ({@code brokerverse.jobs.user-session-sweep-cron}).
 */
@Component
public class AuthSessionSweepJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "USER_SESSION_SWEEP";

  private static final int DEFAULT_TIMEOUT_MINUTES = 30;

  private final UserSessionLog sessions;
  private final SystemParameterService parameters;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param sessions session log
   * @param parameters business parameters (inactivity sign-out)
   * @param cron schedule
   */
  public AuthSessionSweepJob(
      UserSessionLog sessions,
      SystemParameterService parameters,
      @Value("${brokerverse.jobs.user-session-sweep-cron:0 */15 * * * *}") String cron) {
    this.sessions = sessions;
    this.parameters = parameters;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Ends idle, expired, locked and disabled users' sign-in sessions in the session log";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    Duration idle =
        Duration.ofMinutes(
                parameters.intValue(
                    SystemParameterService.SESSION_TIMEOUT_MINUTES, DEFAULT_TIMEOUT_MINUTES))
            .plusSeconds(UserSessionLog.TOUCH_INTERVAL_SECONDS);
    Map<SessionEndReason, Long> ended = sessions.sweep(idle);
    long total = ended.values().stream().mapToLong(Long::longValue).sum();
    String detail =
        ended.isEmpty()
            ? "no session to end"
            : ended.entrySet().stream()
                .map(e -> e.getValue() + " " + e.getKey())
                .sorted()
                .collect(Collectors.joining(", "));
    return new JobOutcome(Math.toIntExact(total), "Sessions ended: " + detail);
  }
}
