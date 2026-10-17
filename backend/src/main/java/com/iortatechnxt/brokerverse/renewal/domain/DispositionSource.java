package com.iortatechnxt.brokerverse.renewal.domain;

/** Who gave a disposition (RENEWAL_DESIGN section 4.3; BRRN.018, RQ14). */
public enum DispositionSource {
  /** A Marketing user on the screen. */
  USER("User"),
  /** A check (non-renewable risk code). */
  SYSTEM_CHECK("System check"),
  /** The decision matrix (AUTO rule). */
  MATRIX("Decision matrix"),
  /** A dispositioned file. */
  UPLOAD("Upload"),
  /** The insurer's response. */
  INSURER("Insurer response"),
  /** A LAMD report. */
  LAMD("LAMD report");

  private final String label;

  DispositionSource(String label) {
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
