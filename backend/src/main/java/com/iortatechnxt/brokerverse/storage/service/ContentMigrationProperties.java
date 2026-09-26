package com.iortatechnxt.brokerverse.storage.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of {@code FILE_BYTEA_MIGRATION} ({@code brokerverse.storage.content-migration.*}).
 *
 * @param cron schedule (Spring cron, UTC); {@code -} (default) = manual runs from the job monitor
 * @param batchSize rows read per query (default 20)
 */
@ConfigurationProperties(prefix = "brokerverse.storage.content-migration")
public record ContentMigrationProperties(String cron, Integer batchSize) {

  private static final int DEFAULT_BATCH = 20;
  private static final int MAX_BATCH = 500;

  /** Applies the defaults. */
  public ContentMigrationProperties {
    cron = cron == null || cron.isBlank() ? "-" : cron.strip();
    batchSize = batchSize == null || batchSize < 1 ? DEFAULT_BATCH : Math.min(batchSize, MAX_BATCH);
  }
}
