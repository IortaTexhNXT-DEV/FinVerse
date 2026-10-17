package com.iortatechnxt.brokerverse.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Where a business record comes from (DATA_MIGRATION_DESIGN sections 10 and 14): created in BIBS,
 * or migrated from a legacy system with its source system, legacy reference and the migration batch
 * that loaded it. Migrated records show a LEGACY badge and an Origin filter in their modules, raise
 * no notification and no outbound integration event, and are undone with their batch.
 *
 * <p>Columns {@code origin}, {@code source_system}, {@code legacy_ref} and {@code migration_batch}
 * of the owning table.
 *
 * @param origin BIBS or MIGRATED
 * @param sourceSystem legacy source system, null for BIBS records
 * @param legacyRef legacy reference (client code, policy number, invoice number...)
 * @param migrationBatch batch number of the load
 */
@Embeddable
public record RecordOrigin(
    @Enumerated(EnumType.STRING) @Column(name = "origin", nullable = false, length = 10)
        Origin origin,
    @Column(name = "source_system", length = 10) String sourceSystem,
    @Column(name = "legacy_ref", length = 80) String legacyRef,
    @Column(name = "migration_batch", length = 20) String migrationBatch) {

  /** A record created in BIBS. */
  public static final RecordOrigin BIBS = new RecordOrigin(Origin.BIBS, null, null, null);

  /** Origin kinds. */
  public enum Origin {
    /** Created in BIBS. */
    BIBS,
    /** Migrated from a legacy system. */
    MIGRATED
  }

  /** A null origin reads as BIBS. */
  public RecordOrigin {
    origin = origin == null ? Origin.BIBS : origin;
  }

  /**
   * A migrated record.
   *
   * @param sourceSystem legacy source system
   * @param legacyRef legacy reference
   * @param migrationBatch loading batch
   * @return origin
   */
  public static RecordOrigin migrated(
      String sourceSystem, String legacyRef, String migrationBatch) {
    return new RecordOrigin(Origin.MIGRATED, sourceSystem, legacyRef, migrationBatch);
  }

  /**
   * Whether the record was migrated.
   *
   * @return true for MIGRATED
   */
  public boolean isMigrated() {
    return origin == Origin.MIGRATED;
  }
}
