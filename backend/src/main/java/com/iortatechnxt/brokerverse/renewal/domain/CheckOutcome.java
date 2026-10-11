package com.iortatechnxt.brokerverse.renewal.domain;

/** Result of one check (BRRN.020). */
public enum CheckOutcome {
  /** The condition holds. */
  PASS("Pass"),
  /** A warning. */
  WARN("Warning"),
  /** The check failed. */
  FAIL("Fail"),
  /** Information only; never blocks. */
  INFO("Information"),
  /** The check does not apply. */
  NOT_APPLICABLE("Not applicable");

  private final String label;

  CheckOutcome(String label) {
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
