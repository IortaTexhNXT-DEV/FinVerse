package com.iortatechnxt.brokerverse.storage.api;

import com.iortatechnxt.brokerverse.storage.api.dto.ContentMigrationCount;
import com.iortatechnxt.brokerverse.storage.service.FileContentMigrationService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reconciliation report of the copy of the files kept in the database to the file store (build step
 * ST1, job {@code FILE_BYTEA_MIGRATION}): per table, the rows with content, the rows copied and the
 * rows left. The report is signed off in each environment before the columns are dropped.
 */
@RestController
@RequestMapping("/api/v1/files/content-migration")
public class ContentMigrationController {

  private static final String MONITOR =
      "hasAnyAuthority('SYSTEM_MONITOR', 'SYSTEM_PARAMETER_MANAGE', 'AUDIT_VIEW')";

  private final FileContentMigrationService migration;

  /**
   * Creates the controller.
   *
   * @param migration the copy
   */
  public ContentMigrationController(FileContentMigrationService migration) {
    this.migration = migration;
  }

  /**
   * Counts per table.
   *
   * @return counts, by table name
   */
  @GetMapping
  @PreAuthorize(MONITOR)
  public List<ContentMigrationCount> counts() {
    return migration.counts().stream().map(ContentMigrationCount::from).toList();
  }
}
