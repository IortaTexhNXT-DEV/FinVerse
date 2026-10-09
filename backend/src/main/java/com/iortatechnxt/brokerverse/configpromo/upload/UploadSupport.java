package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.cache.service.CacheInvalidator;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.configpromo.engine.CanonicalValues;
import com.iortatechnxt.brokerverse.configpromo.engine.TableSchema;
import com.iortatechnxt.brokerverse.configpromo.service.CatalogueService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Database work of the uploads of the configuration screens: lookups for the validation, the
 * preview (add or update by natural key) and the upsert of a row with the audit columns of a
 * maker-checker record (the uploader is the maker, the approver of the upload the checker), an
 * audit entry per record and the clearing of the reference-data caches after an upload.
 */
@Component
// One helper of the 27 uploads: lookups, preview and upsert.
@SuppressWarnings("PMD.GodClass")
public class UploadSupport {
  private static final String VERSION = "version";

  /** Preview: a new record. */
  public static final String ADD = "ADD";

  /** Preview: the record with the same natural key is updated. */
  public static final String UPDATE = "UPDATE";

  private static final String RECORD_STATUS = "record_status";
  private static final String ACTIVE = "ACTIVE";
  private static final java.util.Set<String> NOT_COMPARED =
      java.util.Set.of("authorized_by", "authorized_at", "updated_at", "updated_by");

  private final JdbcTemplate jdbc;
  private final CatalogueService catalogue;
  private final CurrentUser currentUser;
  private final AuditTrailService audit;
  private final CacheInvalidator caches;
  private final Clock clock;

  /**
   * Creates the support.
   *
   * @param jdbc database
   * @param catalogue column types of the tables
   * @param currentUser current user (the approver when the rows are applied)
   * @param audit audit trail
   * @param caches reference-data caches
   * @param clock clock
   */
  public UploadSupport(
      JdbcTemplate jdbc,
      CatalogueService catalogue,
      CurrentUser currentUser,
      AuditTrailService audit,
      CacheInvalidator caches,
      Clock clock) {
    this.jdbc = jdbc;
    this.catalogue = catalogue;
    this.currentUser = currentUser;
    this.audit = audit;
    this.caches = caches;
    this.clock = clock;
  }

  /**
   * Whether a query finds a row.
   *
   * @param sql constant query with bound parameters
   * @param params parameters
   * @return true when found
   */
  public boolean exists(String sql, Object... params) {
    return !jdbc.queryForList(sql, params).isEmpty();
  }

  /**
   * The id a query finds.
   *
   * @param sql constant query returning one id
   * @param params parameters
   * @return id, empty when not found
   */
  public Optional<Long> id(String sql, Object... params) {
    List<Long> ids = jdbc.queryForList(sql, Long.class, params);
    return ids.isEmpty() ? Optional.empty() : Optional.ofNullable(ids.get(0));
  }

  /**
   * The text a query finds.
   *
   * @param sql constant query returning one text
   * @param params parameters
   * @return text, empty when not found
   */
  public Optional<String> text(String sql, Object... params) {
    List<String> values = jdbc.queryForList(sql, String.class, params);
    return values.isEmpty() ? Optional.empty() : Optional.ofNullable(values.get(0));
  }

  /**
   * Rows of a query (export of the current data).
   *
   * @param sql constant query
   * @param params parameters
   * @return rows
   */
  public List<Map<String, Object>> rows(String sql, Object... params) {
    return jdbc.queryForList(sql, params);
  }

  /**
   * The id of a branch of a company by its code.
   *
   * @param companyId company
   * @param code branch code
   * @return id
   */
  public Optional<Long> branchId(Long companyId, String code) {
    return id("select id from org_branch where company_id = ? and code = ?", companyId, code);
  }

  /**
   * Whether an account exists in the chart of accounts of a company.
   *
   * @param companyId company
   * @param code account code
   * @return true when it exists
   */
  public boolean account(Long companyId, String code) {
    return exists("select 1 from coa_account where company_id = ? and code = ?", companyId, code);
  }

  /**
   * Whether a currency exists and is active.
   *
   * @param code currency
   * @return true when usable
   */
  public boolean currency(String code) {
    return exists("select 1 from cur_currency where code = ? and active", code);
  }

  /**
   * Whether a value belongs to a list of values.
   *
   * @param type list
   * @param code value
   * @return true when it exists
   */
  public boolean listValue(String type, String code) {
    return exists("select 1 from lov_value where type_code = ? and code = ?", type, code);
  }

  /**
   * What applying a row would do to the record with its natural key.
   *
   * @param table table
   * @param key natural key column values
   * @return ADD or UPDATE
   */
  public String action(String table, Map<String, Object> key) {
    return find(table, key).isPresent() ? UPDATE : ADD;
  }

  /**
   * The id of the record with a natural key.
   *
   * @param table table with an id column
   * @param key natural key column values
   * @return id, empty when absent
   */
  public Optional<Long> find(String table, Map<String, Object> key) {
    TableSchema schema = schema(table);
    List<Object> params = new ArrayList<>();
    String where = where(schema, key, params);
    String select = schema.has("id") ? "id" : "1";
    List<Map<String, Object>> found =
        jdbc.queryForList(
            "select " + select + " from " + table + " where " + where, params.toArray());
    if (found.isEmpty()) {
      return Optional.empty();
    }
    Object id = found.get(0).values().iterator().next();
    return Optional.of(((Number) id).longValue());
  }

  /**
   * Adds or updates the record of a row (maker-checker audit columns, audit entry).
   *
   * @param table table
   * @param key natural key column values
   * @param values other column values
   * @param context run context (upload number, maker)
   * @param entity record type for the audit trail, e.g. "Branch"
   * @return the id of the record (or 0 for a table without id)
   */
  public long upsert(
      String table,
      Map<String, Object> key,
      Map<String, Object> values,
      BulkContext context,
      String entity) {
    TableSchema schema = schema(table);
    Optional<Long> existing = find(table, key);
    Map<String, Object> all = withApproval(schema, values);
    String maker = context.maker() == null ? currentUser.username() : context.maker();
    String keyText = key.values().stream().map(String::valueOf).collect(Collectors.joining(" / "));
    try {
      if (existing.isPresent()) {
        if (!same(schema, key, all)) {
          putIfPresent(schema, all, "updated_at", clock.instant());
          putIfPresent(schema, all, "updated_by", maker);
          update(schema, key, all);
          audit.record(entity, keyText, AuditAction.UPDATE, "Updated by upload " + context.jobNo());
        }
        return existing.get();
      }
      Map<String, Object> insert = new LinkedHashMap<>(key);
      insert.putAll(all);
      putIfPresent(schema, insert, "created_at", clock.instant());
      putIfPresent(schema, insert, "created_by", maker);
      insert(schema, insert);
      audit.record(entity, keyText, AuditAction.CREATE, "Added by upload " + context.jobNo());
      return find(table, key).orElse(0L);
    } catch (DataAccessException e) {
      throw new BusinessRuleException(
          "CONFIG_UPLOAD_ROW_FAILED",
          keyText
              + " could not be saved: "
              + NestedExceptionUtils.getMostSpecificCause(e).getMessage(),
          e);
    }
  }

  /**
   * The values with the record status and, for an active record, its authorisation by the approver.
   */
  private Map<String, Object> withApproval(TableSchema schema, Map<String, Object> values) {
    Map<String, Object> all = new LinkedHashMap<>(values);
    if (schema.has(RECORD_STATUS)) {
      all.putIfAbsent(RECORD_STATUS, ACTIVE);
    }
    if (schema.has("authorized_by") && ACTIVE.equals(all.get(RECORD_STATUS))) {
      all.put("authorized_by", currentUser.username());
      all.put("authorized_at", clock.instant());
    }
    return all;
  }

  /**
   * Runs a statement of a handler (removal of a link, collection item), turning a database error
   * into a message for the row.
   *
   * @param sql constant statement with bound parameters
   * @param params parameters
   * @return rows changed
   */
  public int execute(String sql, Object... params) {
    try {
      return jdbc.update(sql, params);
    } catch (DataAccessException e) {
      throw new BusinessRuleException(
          "CONFIG_UPLOAD_ROW_FAILED",
          "The row could not be saved: "
              + NestedExceptionUtils.getMostSpecificCause(e).getMessage(),
          e);
    }
  }

  /** Clears the reference-data caches after the rows of an upload were written. */
  public void clearCaches() {
    caches.clearAll();
  }

  /**
   * The current user (the approver while the rows are applied).
   *
   * @return user name
   */
  public String user() {
    return currentUser.username();
  }

  /** Whether the record already holds the values (the same row uploaded again changes nothing). */
  private boolean same(TableSchema schema, Map<String, Object> key, Map<String, Object> values) {
    List<Object> params = new ArrayList<>();
    List<String> conditions = new ArrayList<>();
    for (Map.Entry<String, Object> e : values.entrySet()) {
      if (!NOT_COMPARED.contains(e.getKey())) {
        conditions.add(e.getKey() + " is not distinct from " + placeholder(schema, e.getKey()));
        params.add(param(schema, e.getKey(), e.getValue()));
      }
    }
    String where = where(schema, key, params);
    conditions.add(where);
    return !jdbc.queryForList(
            "select 1 from " + schema.name() + " where " + String.join(" and ", conditions),
            params.toArray())
        .isEmpty();
  }

  private void insert(TableSchema schema, Map<String, Object> values) {
    List<String> columns = new ArrayList<>(values.keySet());
    if (schema.has(VERSION) && !values.containsKey(VERSION)) {
      columns.add(VERSION);
      values.put(VERSION, 0L);
    }
    String sql =
        "insert into "
            + schema.name()
            + " ("
            + String.join(", ", columns)
            + ") values ("
            + columns.stream().map(c -> placeholder(schema, c)).collect(Collectors.joining(", "))
            + ")";
    jdbc.update(sql, params(schema, columns, values).toArray());
  }

  private void update(TableSchema schema, Map<String, Object> key, Map<String, Object> values) {
    List<String> columns = new ArrayList<>(values.keySet());
    String set =
        columns.stream()
            .map(c -> c + " = " + placeholder(schema, c))
            .collect(Collectors.joining(", "));
    if (schema.has(VERSION)) {
      set += ", version = version + 1";
    }
    List<Object> params = params(schema, columns, values);
    String where = where(schema, key, params);
    jdbc.update("update " + schema.name() + " set " + set + " where " + where, params.toArray());
  }

  private String where(TableSchema schema, Map<String, Object> key, List<Object> params) {
    List<String> conditions = new ArrayList<>();
    for (Map.Entry<String, Object> e : key.entrySet()) {
      if (e.getValue() == null) {
        conditions.add(e.getKey() + " is null");
      } else {
        conditions.add(e.getKey() + " = " + placeholder(schema, e.getKey()));
        params.add(param(schema, e.getKey(), e.getValue()));
      }
    }
    return String.join(" and ", conditions);
  }

  private static void putIfPresent(
      TableSchema schema, Map<String, Object> values, String column, Object value) {
    if (schema.has(column)) {
      values.put(column, value);
    }
  }

  private static List<Object> params(
      TableSchema schema, List<String> columns, Map<String, Object> values) {
    List<Object> params = new ArrayList<>();
    for (String c : columns) {
      params.add(param(schema, c, values.get(c)));
    }
    return params;
  }

  private static Object param(TableSchema schema, String column, Object value) {
    if (value == null) {
      return null;
    }
    Object canonical =
        value instanceof java.math.BigDecimal n ? n.stripTrailingZeros().toPlainString() : value;
    return CanonicalValues.parameter(
        canonical instanceof Boolean ? canonical : canonical.toString(),
        schema.column(column).type());
  }

  private static String placeholder(TableSchema schema, String column) {
    return CanonicalValues.placeholder(schema.column(column).type());
  }

  private TableSchema schema(String table) {
    TableSchema schema = catalogue.model().tables().get(table);
    if (schema == null) {
      throw new IllegalStateException("Unknown table " + table);
    }
    return schema;
  }
}
