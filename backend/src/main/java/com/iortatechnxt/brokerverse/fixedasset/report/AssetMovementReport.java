package com.iortatechnxt.brokerverse.fixedasset.report;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** FIN-FA-MOVE: additions, disposals (with gain or loss) and inter-branch transfers of a period. */
@Component
public class AssetMovementReport implements ReportDefinition {

  private static final String SQL =
      """
      select m.movement_type, m.movement_date, a.tag_no, a.description, c.code as category_code,
             fb.code as from_branch, tb.code as to_branch, m.cost, m.accumulated_depreciation,
             m.net_book_value, m.proceeds, m.gain_loss, m.batch_no
      from fa_movement m
      join fa_asset a on a.id = m.asset_id
      join fa_category c on c.id = a.category_id
      left join org_branch fb on fb.id = m.from_branch_id
      left join org_branch tb on tb.id = m.to_branch_id
      where m.company_id = :companyId and m.movement_date between :fromDate and :toDate
        and (cast(:branchId as bigint) is null
             or m.from_branch_id = :branchId or m.to_branch_id = :branchId)
      order by m.movement_type, m.movement_date, a.tag_no
      """;

  private final FixedAssetReportQueries queries;

  /**
   * Creates the report.
   *
   * @param queries report queries
   */
  public AssetMovementReport(FixedAssetReportQueries queries) {
    this.queries = queries;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-FA-MOVE",
        "Asset Movement Schedule",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Additions, disposals and inter-branch transfers of fixed assets in a date range",
        FixedAssetReportQueries.dateRangeParams(),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters params) {
    var sqlParams = FixedAssetReportQueries.dateRange(params);
    List<Map<String, Object>> rows =
        queries.query(
            SQL,
            sqlParams,
            rs -> {
              Map<String, Object> m = new LinkedHashMap<>();
              m.put("type", rs.getString("movement_type"));
              m.put("date", FixedAssetReportQueries.date(rs, "movement_date"));
              m.put("tagNo", rs.getString("tag_no"));
              m.put("description", rs.getString("description"));
              m.put(FixedAssetReportQueries.CATEGORY, rs.getString("category_code"));
              m.put("fromBranch", rs.getString("from_branch"));
              m.put("toBranch", rs.getString("to_branch"));
              m.put(FixedAssetReportQueries.COST, rs.getBigDecimal("cost"));
              m.put(
                  FixedAssetReportQueries.ACCUMULATED,
                  rs.getBigDecimal("accumulated_depreciation"));
              m.put(FixedAssetReportQueries.NET_BOOK_VALUE, rs.getBigDecimal("net_book_value"));
              m.put("proceeds", rs.getBigDecimal("proceeds"));
              m.put("gainLoss", rs.getBigDecimal("gain_loss"));
              return m;
            });
    return TabularReportBuilder.of(params)
        .columns(
            ReportColumn.date("date", "Date"),
            ReportColumn.text("tagNo", "Tag No"),
            ReportColumn.text("description", "Description"),
            ReportColumn.text(FixedAssetReportQueries.CATEGORY, "Category"),
            ReportColumn.text("fromBranch", "Branch"),
            ReportColumn.text("toBranch", "To Branch"),
            ReportColumn.amount(FixedAssetReportQueries.COST, "Cost"),
            ReportColumn.amount(FixedAssetReportQueries.ACCUMULATED, "Accum. Depreciation"),
            ReportColumn.amount(FixedAssetReportQueries.NET_BOOK_VALUE, "Net Book Value"),
            ReportColumn.amount("proceeds", "Proceeds"),
            ReportColumn.amount("gainLoss", "Gain / (Loss)"))
        .groupBy("type", "Movement")
        .rows(rows)
        .presorted()
        .build();
  }
}
