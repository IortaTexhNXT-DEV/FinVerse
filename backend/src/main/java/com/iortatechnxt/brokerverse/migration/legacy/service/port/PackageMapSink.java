package com.iortatechnxt.brokerverse.migration.legacy.service.port;

import com.iortatechnxt.brokerverse.migration.legacy.domain.PackageMapRow.PackageMapEntry;

/**
 * Seam to the Renewal package map read by {@code PACKAGE_REMAP} at Renewal sanitation
 * (DATA_MIGRATION_DESIGN section 15.2; BDOI decision: packages are remapped at Renewal sanitation).
 * The default keeps the entries in {@code mig_package_map} only; the Renewal adapter, added when
 * the Renewal module is merged, also writes them to the Renewal package map with source MIGRATION
 * and withdraws them on a rollback.
 */
public interface PackageMapSink {

  /**
   * Whether a Renewal package map is connected.
   *
   * @return false for the default
   */
  boolean connected();

  /**
   * Hands an entry to the Renewal package map.
   *
   * @param companyId company
   * @param entry entry
   */
  void add(Long companyId, PackageMapEntry entry);

  /**
   * Withdraws an entry of a rolled-back batch.
   *
   * @param companyId company
   * @param entry entry
   */
  void remove(Long companyId, PackageMapEntry entry);
}
