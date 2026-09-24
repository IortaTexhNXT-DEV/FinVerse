package com.iortatechnxt.brokerverse.nbreport.service;

import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/**
 * When an open New Business account counts as stalled (BRNB.115): no stage movement for {@code
 * NB_STALLED_DAYS} days (default 5). Also the "now" of the SLA comparisons, from the clock.
 */
@Component
public class StallRule {

  /** Business parameter holding the threshold in days. */
  public static final String PARAMETER = "NB_STALLED_DAYS";

  private static final int DEFAULT_DAYS = 5;

  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the rule.
   *
   * @param parameters business parameters
   * @param clock clock
   */
  public StallRule(SystemParameterService parameters, Clock clock) {
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * The threshold in days.
   *
   * @return days
   */
  public int days() {
    return parameters.intValue(PARAMETER, DEFAULT_DAYS);
  }

  /**
   * The current time.
   *
   * @return now, in UTC
   */
  public OffsetDateTime now() {
    return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
  }

  /**
   * Accounts that entered their stage before this instant are stalled.
   *
   * @return cut-off
   */
  public OffsetDateTime stalledBefore() {
    return now().minus(Duration.ofDays(days()));
  }
}
