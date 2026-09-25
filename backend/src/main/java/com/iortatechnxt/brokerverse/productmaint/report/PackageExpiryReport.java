package com.iortatechnxt.brokerverse.productmaint.report;

import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionView;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.service.PackageExpiryService;
import com.iortatechnxt.brokerverse.productmaint.service.PackageExpiryService.ExpiringPackage;
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
 * Package Expiry Report (PM-PKG-EXPIRY, BRPM.017): released packages by package end date within n
 * days (default 90), with the anniversary date and the renewal request in progress, if any.
 */
@Component
public class PackageExpiryReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "PM-PKG-EXPIRY";

  private static final String WITHIN = "within";
  private static final int DEFAULT_WITHIN = 90;

  private final PackageExpiryService expiry;

  /**
   * Creates the report.
   *
   * @param expiry expiry list
   */
  public PackageExpiryReport(PackageExpiryService expiry) {
    this.expiry = expiry;
  }

  @Override
  public ReportMetadata metadata() {
    return PmReportSupport.metadata(
        CODE,
        "Package Expiry Report",
        "Released packages by package end date with their renewal status (BRPM.017)",
        ParameterSpec.required(WITHIN, "Ending Within (days)", ParameterType.NUMBER)
            .withDefault(String.valueOf(DEFAULT_WITHIN)));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    int within = p.optionalLong(WITHIN).map(Long::intValue).orElse(DEFAULT_WITHIN);
    List<Map<String, Object>> rows =
        expiry.expiring(p.longValue(PmReportSupport.COMPANY), within).stream()
            .map(PackageExpiryReport::row)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("product", "Product"),
            ReportColumn.text("name", "Package"),
            ReportColumn.count("version", "Version"),
            ReportColumn.date("end_date", "Package End"),
            ReportColumn.count("days_left", "Days Left"),
            ReportColumn.date("anniversary", "Anniversary"),
            ReportColumn.text("renewal", "Renewal Request"),
            ReportColumn.text("renewal_stage", "Renewal Stage"))
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .build();
  }

  private static Map<String, Object> row(ExpiringPackage e) {
    ProductVersionView v = e.version();
    PackageRequest renewal = e.renewal();
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("product", v.productCode());
    row.put("name", v.productName());
    row.put("version", v.versionNo());
    row.put("end_date", v.dates().packageEndDate());
    row.put("days_left", e.daysLeft());
    row.put("anniversary", v.dates().anniversaryDate());
    row.put("renewal", renewal == null ? "None" : renewal.getRequestNo());
    row.put(
        "renewal_stage", renewal == null ? "" : PmReportSupport.label(renewal.getStatus().name()));
    return row;
  }
}
