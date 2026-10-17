package com.iortatechnxt.brokerverse.renewal.domain;

/** Severity a failed check carries (check settings; BRRN.023). */
public enum CheckSeverity {
  /** A failure gives the Exception bucket. */
  FAIL_EXCEPTION("Exception"),
  /** A failure gives the Review bucket. */
  FAIL_REVIEW("Review"),
  /** A warning gives the Review bucket. */
  WARN("Warning"),
  /** Information only. */
  INFO("Information"),
  /** A system action (tag Not for Renewal, routing); no bucket effect. */
  SYSTEM("System");

  private final String label;

  CheckSeverity(String label) {
    this.label = label;
  }

  /**
   * The name shown to users.
   *
   * @return label
   */
  public String label() {
    return label;
  }
}
