package com.iortatechnxt.brokerverse.submitted.service;

import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The business parameters of Submitted Policies (category SUBMITTED; design section 8), read with
 * their delivered defaults.
 */
@Component
public class SbmParameters {

  /** Lead days per segment (segment=days list; * for the others). */
  public static final String LEAD_DAYS = "SBM_RENEWAL_LEAD_DAYS";

  /** Days for the insurer to accept the hold cover. */
  public static final String ACCEPT_DAYS = "SBM_INSURER_ACCEPT_DAYS";

  /** Days before the hold cover ends to alert an unbooked account. */
  public static final String UNBOOKED_DAYS = "SBM_HOLD_COVER_UNBOOKED_ALERT_DAYS";

  /** Days an approval level may wait. */
  public static final String REVIEW_SLA_DAYS = "SBM_REVIEW_SLA_DAYS";

  /** Segments renewed by hand. */
  public static final String MANUAL_SEGMENTS = "SBM_MANUAL_RENEWAL_SEGMENTS";

  private static final int DEFAULT_LEAD_DAYS = 90;
  private static final int DEFAULT_ACCEPT_DAYS = 5;
  private static final int DEFAULT_UNBOOKED_DAYS = 5;

  private final SystemParameterService parameters;

  /**
   * Creates the reader.
   *
   * @param parameters business parameters
   */
  public SbmParameters(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * The lead days of a segment.
   *
   * @param segment segment
   * @return days before expiry of the hand-off
   */
  public int leadDays(String segment) {
    String value = parameters.text(LEAD_DAYS, "");
    int others = DEFAULT_LEAD_DAYS;
    for (String part : value.split(",")) {
      String[] kv = part.split("=");
      if (kv.length == 2 && kv[1].strip().matches("\\d{1,4}")) {
        int days = Integer.parseInt(kv[1].strip());
        if (kv[0].strip().equals(segment)) {
          return days;
        }
        if ("*".equals(kv[0].strip())) {
          others = days;
        }
      }
    }
    return others;
  }

  /**
   * Days for the insurer to accept the hold cover.
   *
   * @return days
   */
  public int acceptDays() {
    return parameters.intValue(ACCEPT_DAYS, DEFAULT_ACCEPT_DAYS);
  }

  /**
   * Days before a hold cover ends to alert an unbooked account.
   *
   * @return days
   */
  public int unbookedAlertDays() {
    return parameters.intValue(UNBOOKED_DAYS, DEFAULT_UNBOOKED_DAYS);
  }

  /**
   * Days an approval level may wait.
   *
   * @return days
   */
  public int reviewSlaDays() {
    return parameters.intValue(REVIEW_SLA_DAYS, DEFAULT_ACCEPT_DAYS);
  }

  /**
   * Segments whose renewal starts by hand.
   *
   * @return segments
   */
  public List<String> manualSegments() {
    return parameters.items(MANUAL_SEGMENTS);
  }
}
