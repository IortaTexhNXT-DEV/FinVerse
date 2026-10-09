package com.iortatechnxt.brokerverse.renewal.domain;

/** Why a renewal needs attention in the listing (Annex BRRN.036; FR-RN-102). */
public enum AttentionFlag {
  /** Not progressing within the escalation days before the expiry. */
  AGEING("Ageing"),
  /** Past its effective expiry date and still open. */
  OVERDUE("Overdue"),
  /** In the Exception bucket or with open claims. */
  HIGH_RISK("High risk");

  private final String label;

  AttentionFlag(String label) {
    this.label = label;
  }

  /**
   * The name shown on the screens.
   *
   * @return label
   */
  public String label() {
    return label;
  }
}
