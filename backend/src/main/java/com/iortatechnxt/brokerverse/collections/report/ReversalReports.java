package com.iortatechnxt.brokerverse.collections.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * "DP PR for Reversal" and "PR 2307 for Reversal" (BRCLXN.024-029, p.59): the accounts tagged in
 * the period with a collector disposition whose Operations action is the DP reversal or the 2307
 * reversal, with the premium and commission figures, the date tagged and the last collection
 * effort. The monthly files cover the previous month, the weekly files Saturday to Friday per sales
 * unit and invoicing branch.
 */
public final class ReversalReports {

  /** DP PR for Reversal code. */
  public static final String DP = "CLX-DP-FOR-REVERSAL";

  /** PR 2307 for Reversal code. */
  public static final String PR2307 = "CLX-PR2307-FOR-REVERSAL";

  private static final String SQL =
      ClxReportSql.ITEM_SELECT
          + ", d.created_at as tagged_at, d.created_by as tagged_by, d.remarks as tag_remarks,"
          + " o.commission, o.vat_on_commission, o.wtax_rate, i.net_outstanding"
          + " - i.outstanding_pr2307 as diff_balance"
          + " from clx_disposition d join clx_item i on i.id = d.item_id"
          + " left join org_branch b on b.id = i.branch_id"
          + " left join ops_invoice o on o.invoice_no = i.invoice_no"
          + " where d.company_id = :companyId and d.ops_action = :action"
          + " and cast(d.created_at at time zone 'Asia/Manila' as date) between :from and :to"
          + ClxReportSql.ITEM_FILTERS
          + " order by d.created_at, i.invoice_no";

  private ReversalReports() {}

  /** A report of the accounts tagged with one Operations action. */
  abstract static class Tagged implements ReportDefinition {

    private final ClxReportSql sql;
    private final ReportMetadata metadata;
    private final String action;

    Tagged(ClxReportSql sql, ReportMetadata metadata, String action) {
      this.sql = sql;
      this.metadata = metadata;
      this.action = action;
    }

    @Override
    public ReportMetadata metadata() {
      return metadata;
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      Map<String, Object> args = ClxReportSql.args(p);
      args.put("action", action);
      List<ReportColumn> columns = new ArrayList<>(ClxReportSql.itemColumns());
      columns.add(ReportColumn.date("tagged_at", "Date Tagged"));
      columns.add(ReportColumn.text("tagged_by", "Tagged By"));
      columns.add(ReportColumn.text("tag_remarks", "Tag Remarks"));
      columns.add(ReportColumn.amount("commission", "Basic Commission"));
      columns.add(ReportColumn.amount("vat_on_commission", "VAT on Commission"));
      columns.add(ReportColumn.amountNoTotal("wtax_rate", "WTAX Rate"));
      columns.add(ReportColumn.amount("diff_balance", "Diff (Premium Balance less PR2307)"));
      return TabularReportBuilder.of(p)
          .columns(columns)
          .rows(sql.rows(SQL, args))
          .presorted()
          .note(ClxReportSql.DRAFT_NOTE)
          .build();
    }
  }

  /** DP PR for Reversal (BRCLXN.024/025/028/029). */
  @Component
  public static class DpForReversal extends Tagged {

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public DpForReversal(ClxReportSql sql) {
      super(
          sql,
          ClxReportSql.metadata(
              DP,
              "DP PR for Reversal",
              "Accounts tagged \"DP PR for reversal\" in the period (BRCLXN.024-029)",
              true),
          "DP_REVERSAL");
    }
  }

  /** PR 2307 for Reversal (BRCLXN.026-029). */
  @Component
  public static class Pr2307ForReversal extends Tagged {

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public Pr2307ForReversal(ClxReportSql sql) {
      super(
          sql,
          ClxReportSql.metadata(
              PR2307,
              "PR 2307 for Reversal",
              "Accounts tagged \"PR 2307 for reversal\" in the period (BRCLXN.026-029)",
              true),
          "CWT2307_REVERSAL");
    }
  }
}
