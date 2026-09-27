package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.renewal.domain.CheckOutcome;
import com.iortatechnxt.brokerverse.renewal.domain.CheckSeverity;

/**
 * The result of one check before it is stored: code, verdict and the severity from the settings.
 *
 * @param code check code
 * @param outcome outcome
 * @param severity severity of the check
 * @param message message
 * @param detail detail
 */
public record Finding(
    String code, CheckOutcome outcome, CheckSeverity severity, String message, String detail) {

  /**
   * Whether the finding counts for the bucket: a failure or a warning of a bucket severity.
   *
   * @return true for FAIL or WARN of severity FAIL_EXCEPTION, FAIL_REVIEW or WARN
   */
  public boolean countsForBucket() {
    boolean bucketSeverity = severity != CheckSeverity.INFO && severity != CheckSeverity.SYSTEM;
    return bucketSeverity && (outcome == CheckOutcome.FAIL || outcome == CheckOutcome.WARN);
  }

  /**
   * Whether the check failed.
   *
   * @return true for FAIL
   */
  public boolean failed() {
    return outcome == CheckOutcome.FAIL;
  }
}
