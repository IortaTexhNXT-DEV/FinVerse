package com.iortatechnxt.brokerverse.productmaint.report;

import com.iortatechnxt.brokerverse.common.security.UserDisplayNames;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.productmaint.service.ProductMatrixQuery;
import com.iortatechnxt.brokerverse.productmaint.service.ProductMatrixQuery.MatrixRow;
import com.iortatechnxt.brokerverse.report.core.NamedExport;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Export of the Product Matrix (BDOI FRS FRPM.003.01, PM-MATRIX): the rows of a tab (Active,
 * Expiring or Expired Products) for the search and filters on screen, with Line of Insurance,
 * Sub-Line, Package Name, Package Description, Insurer, Status, Effective and Expiry Date and the
 * last update.
 */
@Component
public class ProductMatrixReport implements ReportDefinition, NamedExport {

  /** Report code. */
  public static final String CODE = "PM-MATRIX";

  private static final String TAB = "tab";
  private static final String TEXT = "text";
  private static final String LINE = "lineCode";
  private static final String TYPE = "packageType";
  private static final String PACKAGE = "PACKAGE";

  private final ProductMatrixQuery query;
  private final UserDisplayNames names;

  /**
   * Creates the report.
   *
   * @param query Product Matrix reads
   * @param names user names
   */
  public ProductMatrixReport(ProductMatrixQuery query, UserDisplayNames names) {
    this.query = query;
    this.names = names;
  }

  @Override
  public String exportName() {
    return "Product Matrix";
  }

  @Override
  public ReportMetadata metadata() {
    return PmReportSupport.metadata(
        CODE,
        "Product Matrix",
        "Maintained packages and products with their line, insurers, status and dates",
        ParameterSpec.select(TAB, "Tab", List.of("ACTIVE", "EXPIRING", "EXPIRED"), "ACTIVE"),
        ParameterSpec.optional(TEXT, "Search", ParameterType.TEXT),
        ParameterSpec.optional(LINE, "Line of Insurance", ParameterType.TEXT),
        ParameterSpec.select(
            TYPE,
            "Product Type",
            List.of(PmReportSupport.ALL, PACKAGE, "NON_PACKAGE"),
            PmReportSupport.ALL));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    String type = PmReportSupport.selected(p, TYPE);
    List<Map<String, Object>> rows =
        query
            .all(
                new ProductMatrixQuery.Filter(
                    p.optionalText(TAB).orElse("ACTIVE"),
                    p.optionalText(TEXT).orElse(null),
                    PmReportSupport.upper(p, LINE),
                    type == null ? null : PACKAGE.equals(type),
                    null,
                    null))
            .stream()
            .map(this::row)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("line", "Line of Insurance"),
            ReportColumn.text("subLine", "Sub-Line"),
            ReportColumn.text("code", "Risk Code"),
            ReportColumn.text("name", "Package Name"),
            ReportColumn.text("description", "Package Description"),
            ReportColumn.text("type", "Product Type"),
            ReportColumn.text("insurers", "Insurer"),
            ReportColumn.text("status", "Status"),
            ReportColumn.date("effective", "Effective Date"),
            ReportColumn.date("expiry", "Expiry Date"),
            ReportColumn.text("updatedBy", "Last Updated By"),
            ReportColumn.date("updatedAt", "Last Updated Date"))
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .build();
  }

  private Map<String, Object> row(MatrixRow r) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("line", r.lineOfInsurance());
    row.put("subLine", r.subLine());
    row.put("code", r.productCode());
    row.put("name", r.packageName());
    row.put("description", r.description());
    row.put("type", r.productType());
    row.put("insurers", r.insurers());
    row.put("status", r.status());
    row.put("effective", r.effectiveDate());
    row.put("expiry", r.expiryDate());
    row.put("updatedBy", names.displayName(r.lastUpdatedBy()));
    row.put(
        "updatedAt", r.lastUpdatedAt() == null ? null : BusinessClock.dateOf(r.lastUpdatedAt()));
    return row;
  }
}
