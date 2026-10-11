package com.iortatechnxt.brokerverse.security.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Reports an unavailable security store (token denylist, session log, rate limit counters): every
 * failure is logged at ERROR and counted in the metric {@value #METRIC} (tag {@code store}, for the
 * monitoring alert rule), and an {@link SecurityStoreUnavailable} event is published at most once a
 * minute per store for the in-application alert. Reporting never throws.
 */
@Component
public class SecurityStoreAlarm {

  /** Metric counting the failures of the security stores. */
  public static final String METRIC = "brokerverse.security.store.failures";

  private static final Logger LOG = LoggerFactory.getLogger(SecurityStoreAlarm.class);
  private static final Duration EVENT_INTERVAL = Duration.ofMinutes(1);

  private final MeterRegistry meters;
  private final ApplicationEventPublisher events;
  private final Clock clock;
  private final Map<String, Instant> lastEvent = new ConcurrentHashMap<>();

  /**
   * Creates the alarm.
   *
   * @param meters metrics
   * @param events event publisher (the in-application alert)
   * @param clock clock
   */
  public SecurityStoreAlarm(MeterRegistry meters, ApplicationEventPublisher events, Clock clock) {
    this.meters = meters;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Reports a failure of a store.
   *
   * @param store store name ({@code denylist}, {@code sessions}, {@code counters})
   * @param consequence what the application does meanwhile
   * @param error the failure
   */
  public void raise(String store, String consequence, RuntimeException error) {
    LOG.error("Security store '{}' unavailable; {}", store, consequence, error);
    try {
      meters.counter(METRIC, "store", store).increment();
      Instant now = clock.instant();
      Instant previous = lastEvent.get(store);
      if (previous == null || !now.isBefore(previous.plus(EVENT_INTERVAL))) {
        lastEvent.put(store, now);
        events.publishEvent(
            new SecurityStoreUnavailable(store, consequence, String.valueOf(error.getMessage())));
      }
    } catch (RuntimeException reportFailure) {
      LOG.error("Failure of security store '{}' could not be reported", store, reportFailure);
    }
  }
}
