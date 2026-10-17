package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.storage.service.FileContentMigrationService.TableRun;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * {@code FILE_BYTEA_MIGRATION}: the one-off copy of the file content kept in module {@code bytea}
 * columns to the file store (build step ST1), table by table, in batches, with the SHA-256 checked
 * on every row ({@link FileContentMigrationService}). Manual by default; run it from the job
 * monitor until every table shows nothing left, then sign off the reconciliation report ({@code GET
 * /api/v1/files/content-migration}) before the later migration drops the columns. The run message
 * lists, per table, the rows copied, failed and left.
 */
@Component
@EnableConfigurationProperties(ContentMigrationProperties.class)
public class FileContentMigrationJob implements ManagedJob {

  /** Job name. */
  public static final String NAME = "FILE_BYTEA_MIGRATION";

  private final FileContentMigrationService migration;
  private final ContentMigrationProperties properties;

  /**
   * Creates the job.
   *
   * @param migration the copy
   * @param properties schedule and batch size
   */
  public FileContentMigrationJob(
      FileContentMigrationService migration, ContentMigrationProperties properties) {
    this.migration = migration;
    this.properties = properties;
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String description() {
    return "Copies the files still kept in the database to the file store and checks each copy";
  }

  @Override
  public String cron() {
    return properties.cron();
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int moved = 0;
    int failed = 0;
    List<String> lines = new ArrayList<>();
    for (LegacyFileTable table : migration.tables()) {
      TableRun run = migration.migrate(table, properties.batchSize());
      moved += run.moved();
      failed += run.failed();
      if (run.moved() > 0 || run.failed() > 0 || run.remaining() > 0) {
        lines.add(
            run.table()
                + " "
                + run.moved()
                + " copied, "
                + run.failed()
                + " failed, "
                + run.remaining()
                + " left");
      }
    }
    String summary = moved + " files copied, " + failed + " failed";
    return new JobOutcome(
        moved,
        lines.isEmpty() ? summary + "; nothing left" : summary + ": " + String.join("; ", lines));
  }
}
