package com.iortatechnxt.brokerverse.migration.signoff.domain;

/**
 * Sign-off gates of a data object (DATA_MIGRATION_DESIGN section 13; FR-DM-003), signed in order.
 */
public enum Gate {
  /** Decision of the class (data owner). */
  G1("Decision"),
  /** Mapping approved and layout frozen (data owner). */
  G2("Mapping"),
  /** Validation within the error threshold (Data Steward; waivers by the data owner). */
  G3("Validation"),
  /** Load approved (Data Migration Lead). */
  G4("Load approval"),
  /** Reconciliation matched or explained (reconciliation approver). */
  G5("Reconciliation"),
  /** Object accepted on screen samples (data owner and Data Migration Lead). */
  G6("Object accepted"),
  /** Go-live (go / no-go board). */
  G7("Go-live");

  private final String label;

  Gate(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
