package com.iortatechnxt.brokerverse.storage.api.dto;

import com.iortatechnxt.brokerverse.storage.service.FileContentMigrationService.TableCount;

/**
 * Reconciliation counts of one table.
 *
 * @param table table name
 * @param total rows with file content
 * @param moved rows copied to the file store
 * @param remaining rows still to copy
 */
public record ContentMigrationCount(String table, long total, long moved, long remaining) {

  /**
   * Maps the counts.
   *
   * @param count counts
   * @return response
   */
  public static ContentMigrationCount from(TableCount count) {
    return new ContentMigrationCount(
        count.table(), count.total(), count.moved(), count.remaining());
  }
}
