package com.iortatechnxt.brokerverse.productmaint.report;

import com.iortatechnxt.brokerverse.common.security.UserDisplayNames;
import com.iortatechnxt.brokerverse.productmaint.service.PlacementUpdateQuery;
import com.iortatechnxt.brokerverse.productmaint.service.PlacementUpdateQuery.PlacementRow;
import com.iortatechnxt.brokerverse.report.core.ColumnType;
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
 * Consolidated Placement Update Report (BDOI FRS FRPM.007.01, PM-PLACEMENT-UPDATE): the quotation
 * requests active or processed in the reporting period with BDOI's twelve columns, exported as
 * {@code PlacementUpdate_MMDDYYYY.xlsx}; generated weekly by the job PLACEMENT_UPDATE_REPORT and on
 * request, and kept in the report repository.
 */
@Component
public class ConsolidatedPlacementReport implements ReportDefinition, NamedExport {

  /** Report code. */
  public static final String CODE = "PM-PLACEMENT-UPDATE";

  /** First day of the period. */
  public static final String FROM = "from";

  /** Last day of the period. */
  public static final String TO = "to";

  /** Scope. */
  public static final String SCOPE = "scope";

  /** The columns, in BDOI's order. */
  public static final List<ReportColumn> COLUMNS =
      List.of(
          ReportColumn.text("itemNo", "Item No."),
          ReportColumn.text("insured", "Insured's Name"),
          ReportColumn.text("segment", "Marketing Segment"),
          ReportColumn.text("team", "Team"),
          ReportColumn.text("accountOfficer", "Account Officer"),
          ReportColumn.text("tsuHandler", "TSU Handler"),
          ReportColumn.text("line", "Line of Insurance"),
          ReportColumn.text("subLine", "Sub-Line"),
          ReportColumn.date("receivedDate", "PRF Received Date"),
          ReportColumn.text("receivedTime", "PRF Received Time"),
          new ReportColumn("aging", "Aging (Days)", ColumnType.NUMBER, false),
          ReportColumn.text("status", "Status"));

  private final PlacementUpdateQuery query;
  private final UserDisplayNames names;

  /**
   * Creates the report.
   *
   * @param query report rows
   * @param names user names
   */
  public ConsolidatedPlacementReport(PlacementUpdateQuery query, UserDisplayNames names) {
    this.query = query;
    this.names = names;
  }

  @Override
  public String exportName() {
    return "PlacementUpdate";
  }

  @Override
  public ReportMetadata metadata() {
    return PmReportSupport.metadata(
        CODE,
        "Consolidated Placement Update Report",
        "Quotation requests active or processed in the period with their latest status",
        ParameterSpec.required(FROM, "Period From", ParameterType.DATE),
        ParameterSpec.required(TO, "Period To", ParameterType.DATE),
        ParameterSpec.select(
            SCOPE,
            "Scope",
            List.of(PlacementUpdateQuery.QUOTATION, PlacementUpdateQuery.QUOTATION_AND_PACKAGE),
            PlacementUpdateQuery.QUOTATION));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<Map<String, Object>> rows =
        query
            .rows(
                p.longValue(PmReportSupport.COMPANY),
                p.date(FROM),
                p.date(TO),
                p.optionalText(SCOPE).orElse(PlacementUpdateQuery.QUOTATION))
            .stream()
            .map(this::cells)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(COLUMNS)
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .build();
  }

  /**
   * The cells of a row as shown and exported.
   *
   * @param r row
   * @return cells by column key
   */
  public Map<String, Object> cells(PlacementRow r) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("itemNo", String.valueOf(r.itemNo()));
    row.put("insured", r.insuredName());
    row.put("segment", r.segment());
    row.put("team", r.team());
    row.put("accountOfficer", names.displayName(r.accountOfficer()));
    row.put("tsuHandler", r.tsuHandler() == null ? null : names.displayName(r.tsuHandler()));
    row.put("line", r.line());
    row.put("subLine", r.subLine());
    row.put("receivedDate", r.receivedDate());
    row.put("receivedTime", r.receivedTime() == null ? null : r.receivedTime().toString());
    row.put("aging", r.agingDays());
    row.put("status", r.status());
    return row;
  }
}
