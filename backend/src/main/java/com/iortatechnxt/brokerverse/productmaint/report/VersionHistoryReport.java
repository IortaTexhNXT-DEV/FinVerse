package com.iortatechnxt.brokerverse.productmaint.report;

import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueryService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionView;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Package Version History (PM-VERSION-HISTORY, BRPM.006/007 audit, PMADD06): every version of the
 * packaged products (or of one product), newest first per product, with status, effective dates,
 * package end date, the maker who submitted it, the validator and when, the source request and the
 * change summary. Read through the catalog's {@link ProductVersionQueryService}.
 */
@Component
public class VersionHistoryReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "PM-VERSION-HISTORY";

  private static final String PRODUCT = "product";

  private static final String PACKAGES =
      "select code from cat_product where packaged"
          + " and (cast(:product as varchar) is null or code = :product) order by code";

  private final ProductVersionQueryService versions;
  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the report.
   *
   * @param versions catalog package versions
   * @param jdbc JDBC template (packaged product codes)
   */
  public VersionHistoryReport(
      ProductVersionQueryService versions, NamedParameterJdbcTemplate jdbc) {
    this.versions = versions;
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return PmReportSupport.metadata(
        CODE,
        "Package Version History",
        "Versions of the packaged products with validator, dates and change summary"
            + " (BRPM.006/007, PMADD06)",
        ParameterSpec.optional(PRODUCT, "Product Code", ParameterType.TEXT));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<String> codes =
        jdbc.queryForList(
            PACKAGES,
            new MapSqlParameterSource(PRODUCT, PmReportSupport.upper(p, PRODUCT)),
            String.class);
    List<Map<String, Object>> rows =
        codes.stream()
            .flatMap(code -> versions.versions(code).stream())
            .map(VersionHistoryReport::row)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(PRODUCT, "Product"),
            ReportColumn.count("version", "Version"),
            ReportColumn.text("status", "Status"),
            ReportColumn.date("effective_from", "Effective From"),
            ReportColumn.date("effective_to", "Effective To"),
            ReportColumn.date("end_date", "Package End"),
            ReportColumn.text("submitted_by", "Submitted By"),
            ReportColumn.text("validated_by", "Validated By"),
            ReportColumn.date("validated_at", "Validated On"),
            ReportColumn.text("request", "Source Request"),
            ReportColumn.text("summary", "Change Summary"))
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .build();
  }

  private static Map<String, Object> row(ProductVersionView v) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(PRODUCT, v.productCode());
    row.put("version", v.versionNo());
    row.put("status", PmReportSupport.label(v.status().name()));
    row.put("effective_from", v.dates() == null ? null : v.dates().effectiveFrom());
    row.put("effective_to", v.effectiveTo());
    row.put("end_date", v.dates() == null ? null : v.dates().packageEndDate());
    row.put("submitted_by", v.checkpoint() == null ? null : v.checkpoint().submittedBy());
    row.put("validated_by", v.checkpoint() == null ? null : v.checkpoint().validatedBy());
    row.put("validated_at", v.checkpoint() == null ? null : date(v.checkpoint().validatedAt()));
    row.put("request", v.origin() == null ? null : v.origin().sourceRequestNo());
    row.put("summary", v.origin() == null ? null : v.origin().changeSummary());
    return row;
  }

  private static LocalDate date(Instant instant) {
    return instant == null ? null : instant.atZone(PmReportSupport.MANILA).toLocalDate();
  }
}
