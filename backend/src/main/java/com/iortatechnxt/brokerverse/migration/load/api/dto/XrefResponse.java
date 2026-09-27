package com.iortatechnxt.brokerverse.migration.load.api.dto;

import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import java.time.Instant;

/**
 * A legacy key cross-reference (DATA_MIGRATION_DESIGN section 11).
 *
 * @param sourceSystem source system
 * @param objectCode object
 * @param legacyKey legacy key
 * @param targetEntity BIBS record type
 * @param targetCode BIBS record code
 * @param batchNo loading batch
 * @param loadedAt time
 * @param rolledBackAt rollback time, if undone
 */
public record XrefResponse(
    String sourceSystem,
    String objectCode,
    String legacyKey,
    String targetEntity,
    String targetCode,
    String batchNo,
    Instant loadedAt,
    Instant rolledBackAt) {

  /**
   * Maps an entry.
   *
   * @param x entry
   * @param batchNo its batch
   * @return response
   */
  public static XrefResponse from(KeyXref x, String batchNo) {
    return new XrefResponse(
        x.getSourceSystem(),
        x.getObjectCode(),
        x.getLegacyKey(),
        x.getTargetEntity(),
        x.getTargetCode(),
        batchNo,
        x.getLoadedAt(),
        x.getRolledBackAt());
  }
}
