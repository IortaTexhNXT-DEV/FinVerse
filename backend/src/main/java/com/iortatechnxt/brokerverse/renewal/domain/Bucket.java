package com.iortatechnxt.brokerverse.renewal.domain;

/** Classification of a candidate after its checks (BRRN.023): the pill green / yellow / red. */
public enum Bucket {
  /** No check failed or warned. */
  CLEAN("Clean"),
  /** A check needs a review. */
  REVIEW("Review"),
  /** A check failed with an exception. */
  EXCEPTION("Exception");

  private final String label;

  Bucket(String label) {
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
