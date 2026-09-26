package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.common.storage.FileStore;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Copies the file content still kept in module {@code bytea} columns to the file store (build step
 * ST1, DOCUMENT_STORAGE_DECISION section 7): per row, in its own transaction, the content is stored
 * through {@link StoredFileService#storeChecked}, read back from the store and its SHA-256 compared
 * with the content read from the table (and with the checksum the module recorded), and only then
 * the row's {@code stored_file_id} is set. A failing row is left as it is and counted; the next run
 * takes it again. Rows already copied are never read again, so the copy is restartable and a second
 * run changes nothing. The {@code bytea} columns stay until a later migration drops them.
 */
@Component
public class FileContentMigrationService {

  private static final Logger LOG = LoggerFactory.getLogger(FileContentMigrationService.class);

  private final List<LegacyFileTable> tables;
  private final StoredFileService files;
  private final FileStore store;
  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;

  /**
   * Creates the service.
   *
   * @param tables tables declared by the modules, in bean order ({@code @Order}: a narrower table
   *     that must take its rows first comes before the general one)
   * @param files stored files
   * @param store object store (read-back check)
   * @param jdbc JDBC access to the module tables
   * @param transactions transaction manager (one transaction per row)
   */
  public FileContentMigrationService(
      List<LegacyFileTable> tables,
      StoredFileService files,
      FileStore store,
      JdbcTemplate jdbc,
      PlatformTransactionManager transactions) {
    this.tables = List.copyOf(tables);
    this.files = files;
    this.store = store;
    this.jdbc = jdbc;
    this.tx = new TransactionTemplate(transactions);
  }

  /**
   * The declared tables, by name.
   *
   * @return tables
   */
  public List<LegacyFileTable> tables() {
    return tables;
  }

  /**
   * The reconciliation report: per table, the rows with content and how many are copied.
   *
   * @return counts, by table name
   */
  public List<TableCount> counts() {
    return tables.stream().map(this::count).toList();
  }

  private TableCount count(LegacyFileTable table) {
    Map<String, Object> row = jdbc.queryForMap(table.countSql());
    long total = ((Number) row.get("total")).longValue();
    long moved = ((Number) row.get("moved")).longValue();
    return new TableCount(table.table(), total, moved, total - moved);
  }

  /**
   * Copies every remaining row of one table.
   *
   * @param table table
   * @param batchSize rows read per query
   * @return rows copied and failed, and rows left afterwards
   */
  public TableRun migrate(LegacyFileTable table, int batchSize) {
    long after = Long.MIN_VALUE;
    int moved = 0;
    int failed = 0;
    List<LegacyRow> batch = next(table, after, batchSize);
    while (!batch.isEmpty()) {
      for (LegacyRow row : batch) {
        after = row.key();
        if (moveOne(table, row)) {
          moved++;
        } else {
          failed++;
        }
      }
      batch = next(table, after, batchSize);
    }
    return new TableRun(table.table(), moved, failed, count(table).remaining());
  }

  private List<LegacyRow> next(LegacyFileTable table, long after, int batchSize) {
    return jdbc.query(table.selectSql(), (rs, n) -> row(rs), after, batchSize);
  }

  private boolean moveOne(LegacyFileTable table, LegacyRow row) {
    try {
      tx.executeWithoutResult(s -> copy(table, row));
      return true;
    } catch (RuntimeException e) {
      LOG.warn("Content of {} row {} not copied: {}", table.table(), row.key(), e.getMessage());
      return false;
    }
  }

  private void copy(LegacyFileTable table, LegacyRow row) {
    byte[] content = row.content();
    String sha256 = Sha256.hex(content);
    if (row.recordedSha256() != null && !sameHash(row.recordedSha256(), sha256)) {
      throw new IllegalStateException("content does not match the recorded SHA-256");
    }
    StoredFile file =
        files.storeChecked(
            new StoreRequest(
                new FileOwner(row.companyId(), table.ownerType(), row.ownerId()),
                row.documentType(),
                row.recordClass(),
                row.fileName(),
                content,
                sha256),
            row.contentType(),
            table.origin());
    if (!sameHash(Sha256.hex(store.get(file.objectRef())), sha256)) {
      throw new IllegalStateException("stored object does not match the SHA-256 of the row");
    }
    if (jdbc.update(table.attachSql(), file.getId(), row.key()) != 1) {
      throw new IllegalStateException("row was copied by another run");
    }
  }

  private static boolean sameHash(String a, String b) {
    try {
      return MessageDigest.isEqual(HexFormat.of().parseHex(a), HexFormat.of().parseHex(b));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static LegacyRow row(ResultSet rs) throws SQLException {
    long company = rs.getLong("company_id");
    Long companyId = rs.wasNull() ? null : company;
    return new LegacyRow(
        rs.getLong("file_key"),
        companyId,
        rs.getString("owner_id"),
        rs.getString("record_class"),
        rs.getString("document_type"),
        rs.getString("file_name"),
        rs.getString("content_type"),
        rs.getString("recorded_sha256"),
        rs.getBytes("content"));
  }

  /**
   * Reconciliation counts of a table.
   *
   * @param table table name
   * @param total rows with content
   * @param moved rows copied to the file store
   * @param remaining rows still to copy
   */
  public record TableCount(String table, long total, long moved, long remaining) {}

  /**
   * Result of the copy of one table.
   *
   * @param table table name
   * @param moved rows copied in this run
   * @param failed rows that failed in this run (left for the next run)
   * @param remaining rows still to copy afterwards
   */
  public record TableRun(String table, int moved, int failed, long remaining) {}

  /** One row read from a table. */
  private record LegacyRow(
      long key,
      Long companyId,
      String ownerId,
      String recordClass,
      String documentType,
      String fileName,
      String contentType,
      String recordedSha256,
      byte[] content) {}
}
