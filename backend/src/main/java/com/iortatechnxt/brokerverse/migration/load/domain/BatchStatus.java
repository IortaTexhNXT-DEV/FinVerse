package com.iortatechnxt.brokerverse.migration.load.domain;

import java.util.EnumSet;
import java.util.Set;

/** Status of a migration batch (DATA_MIGRATION_DESIGN section 4). */
public enum BatchStatus {
  PLANNED,
  VALIDATED,
  APPROVED,
  LOADING,
  LOADED,
  LOADED_WITH_REJECTS,
  FAILED,
  RECONCILED,
  SIGNED_OFF,
  ROLLBACK_REQUESTED,
  ROLLING_BACK,
  ROLLED_BACK;

  private static final Set<BatchStatus> LOADED_STATES =
      EnumSet.of(LOADED, LOADED_WITH_REJECTS, RECONCILED, ROLLBACK_REQUESTED);

  /**
   * Whether the batch holds loaded records (reconcile, sign off or roll back).
   *
   * @return true after a load and before sign-off or rollback
   */
  public boolean holdsLoad() {
    return LOADED_STATES.contains(this);
  }
}
