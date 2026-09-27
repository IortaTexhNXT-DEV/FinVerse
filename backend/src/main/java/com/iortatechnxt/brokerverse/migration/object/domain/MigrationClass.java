package com.iortatechnxt.brokerverse.migration.object.domain;

/**
 * Class of a data object decided at gate G1 (BRID 1.1a; DATA_MIGRATION_DESIGN section 1): what the
 * migration does with the object.
 */
public enum MigrationClass {
  /** Master or reference data loaded into BIBS through the owning service. */
  MIGRATE,
  /** Open items loaded with their open position at cutover. */
  CARRY_FORWARD,
  /** History kept read-only in the legacy archive. */
  ARCHIVE,
  /** Not migrated. */
  EXCLUDED,
  /** Migrated only when the recorded condition is met. */
  CONDITIONAL;

  /**
   * Whether an object of the class may be loaded into BIBS business tables.
   *
   * @param conditionMet the condition of a conditional object is met
   * @return true for MIGRATE, CARRY_FORWARD and a met CONDITIONAL
   */
  public boolean loadable(boolean conditionMet) {
    return this == MIGRATE || this == CARRY_FORWARD || (this == CONDITIONAL && conditionMet);
  }
}
