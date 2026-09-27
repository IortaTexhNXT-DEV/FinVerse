package com.iortatechnxt.brokerverse.migration.report;

import com.iortatechnxt.brokerverse.common.security.UserDisplayNames;
import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.nbreport.service.SqlArgs;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * A Data Migration report (DATA_MIGRATION_DESIGN section 21; category "Data Migration", viewed and
 * exported with {@code MIG_VIEW} unless stated): one SQL over the migration tables with the company
 * and the optional object and batch filters ({@code :company}, {@code :object}, {@code :batch}),
 * its columns and an optional grouping.
 */
public final class MigReport implements ReportDefinition {

  /** Company parameter. */
  public static final String COMPANY = "companyId";

  /** Object filter. */
  public static final String OBJECT = "object";

  /** Batch filter. */
  public static final String BATCH = "batch";

  /** SQL filter on an object column {@code x.object_code}. */
  public static final String OBJECT_FILTER =
      " and (cast(:object as varchar) is null or x.object_code = :object)";

  /** SQL filter on a batch number column {@code b.batch_no}. */
  public static final String BATCH_FILTER =
      " and (cast(:batch as varchar) is null or b.batch_no = :batch)";

  /** Readable time in Philippine time of a timestamptz column. */
  public static final String PHT = " at time zone 'Asia/Manila', 'YYYY-MM-DD HH24:MI')";

  private final ReportMetadata metadata;
  private final String sql;
  private final List<ReportColumn> columns;
  private final String groupKey;
  private final String groupLabel;
  private final UnaryOperator<Map<String, Object>> row;
  private final NbReportJdbc jdbc;
  private final List<ParameterSpec> extra;

  private MigReport(Builder b) {
    this.metadata = b.metadata();
    this.sql = b.sql;
    this.columns = List.copyOf(b.columns);
    this.groupKey = b.groupKey;
    this.groupLabel = b.groupLabel;
    this.row = b.row;
    this.jdbc = b.jdbc;
    this.extra = List.copyOf(b.extra);
  }

  /**
   * Starts a report.
   *
   * @param jdbc report SQL
   * @param code code
   * @param title title
   * @param description purpose
   * @return builder
   */
  public static Builder of(NbReportJdbc jdbc, String code, String title, String description) {
    return new Builder(jdbc, code, title, description);
  }

  @Override
  public ReportMetadata metadata() {
    return metadata;
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    SqlArgs args =
        SqlArgs.company(p.longValue(COMPANY))
            .with(OBJECT, upper(p, OBJECT))
            .with(BATCH, upper(p, BATCH));
    for (ParameterSpec spec : extra) {
      args =
          args.with(
              spec.name(),
              spec.type() == ParameterType.DATE
                  ? p.optionalDate(spec.name()).orElse(null)
                  : upper(p, spec.name()));
    }
    List<Map<String, Object>> rows = jdbc.rows(sql, args.map()).stream().map(row).toList();
    TabularReportBuilder builder =
        TabularReportBuilder.of(p).columns(columns).rows(rows).presorted();
    if (groupKey != null) {
      builder.groupBy(groupKey, groupLabel);
    }
    return builder.build();
  }

  private static String upper(ReportParameters p, String name) {
    return p.optionalText(name)
        .map(String::strip)
        .filter(v -> !v.isEmpty() && !"ALL".equalsIgnoreCase(v))
        .map(v -> v.toUpperCase(Locale.ROOT))
        .orElse(null);
  }

  /**
   * A readable label of a code ("LOADED_WITH_REJECTS" to "Loaded with rejects").
   *
   * @param code code
   * @return label
   */
  public static String label(Object code) {
    if (code == null) {
      return "";
    }
    String text = code.toString().replace('_', ' ').toLowerCase(Locale.ROOT);
    return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

  /**
   * Replaces code cells by their labels.
   *
   * @param keys cells
   * @return row function
   */
  public static UnaryOperator<Map<String, Object>> relabel(String... keys) {
    return r -> {
      for (String key : keys) {
        r.put(key, label(r.get(key)));
      }
      return r;
    };
  }

  /** Builder of a report. */
  public static final class Builder {

    private final NbReportJdbc jdbc;
    private final String code;
    private final String title;
    private final String description;
    private final List<ReportColumn> columns = new ArrayList<>();
    private final List<ParameterSpec> extra = new ArrayList<>();
    private String sql;
    private String groupKey;
    private String groupLabel;
    private boolean objectFilter;
    private boolean batchFilter;
    private Permission permission = Permission.MIG_VIEW;
    private ReportCategory category = ReportCategory.DATA_MIGRATION;
    private UnaryOperator<Map<String, Object>> row = UnaryOperator.identity();

    private Builder(NbReportJdbc jdbc, String code, String title, String description) {
      this.jdbc = jdbc;
      this.code = code;
      this.title = title;
      this.description = description;
    }

    /**
     * The SQL.
     *
     * @param value SQL with named parameters
     * @return this
     */
    public Builder sql(String value) {
      this.sql = value;
      return this;
    }

    /**
     * The columns.
     *
     * @param cols columns
     * @return this
     */
    public Builder columns(ReportColumn... cols) {
      columns.addAll(List.of(cols));
      return this;
    }

    /**
     * Groups the rows.
     *
     * @param key column
     * @param label label
     * @return this
     */
    public Builder groupBy(String key, String label) {
      this.groupKey = key;
      this.groupLabel = label;
      return this;
    }

    /**
     * Offers the object filter.
     *
     * @return this
     */
    public Builder byObject() {
      this.objectFilter = true;
      return this;
    }

    /**
     * Offers the batch filter.
     *
     * @return this
     */
    public Builder byBatch() {
      this.batchFilter = true;
      return this;
    }

    /**
     * A further parameter.
     *
     * @param spec parameter
     * @return this
     */
    public Builder parameter(ParameterSpec spec) {
      extra.add(spec);
      return this;
    }

    /**
     * A permission other than {@code MIG_VIEW}.
     *
     * @param value permission
     * @return this
     */
    public Builder permission(Permission value) {
      this.permission = value;
      return this;
    }

    /**
     * A category other than Data Migration.
     *
     * @param value category
     * @return this
     */
    public Builder category(ReportCategory value) {
      this.category = value;
      return this;
    }

    /**
     * Post-processing of each row.
     *
     * @param value row function
     * @return this
     */
    public Builder row(UnaryOperator<Map<String, Object>> value) {
      this.row = value;
      return this;
    }

    /**
     * Shows users by their display name, never by login id: the cells of the given columns hold
     * login ids and are replaced by the users' names (the login id when the user is unknown).
     *
     * @param names display names of users
     * @param keys columns holding login ids
     * @return this
     */
    public Builder users(UserDisplayNames names, String... keys) {
      UnaryOperator<Map<String, Object>> before = this.row;
      List<String> cells = List.of(keys);
      this.row =
          r -> {
            Map<String, Object> out = before.apply(r);
            for (String key : cells) {
              Object login = out.get(key);
              if (login != null) {
                String name = names.displayName(login.toString());
                out.put(key, name == null ? login : name);
              }
            }
            return out;
          };
      return this;
    }

    /**
     * Builds the report.
     *
     * @return report
     */
    public MigReport build() {
      return new MigReport(this);
    }

    private ReportMetadata metadata() {
      List<ParameterSpec> params = new ArrayList<>();
      params.add(ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY));
      if (objectFilter) {
        params.add(ParameterSpec.optional(OBJECT, "Data object", ParameterType.TEXT));
      }
      if (batchFilter) {
        params.add(ParameterSpec.optional(BATCH, "Batch", ParameterType.TEXT));
      }
      params.addAll(extra);
      return new ReportMetadata(
          code, title, category, description, params, permission, permission, true);
    }
  }
}
