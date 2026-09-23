package com.iortatechnxt.finverse.reserves.report;

import static com.iortatechnxt.finverse.reserves.report.ReserveReportSupport.K_BRANCH;

import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.reserves.domain.RunStatus;
import com.iortatechnxt.finverse.reserves.domain.TakafulItem;
import com.iortatechnxt.finverse.reserves.domain.ValuationRun;
import com.iortatechnxt.finverse.reserves.service.ValuationRunService;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR074 Surplus / Mudharabah Payment: the takaful surplus of policies expiring in the months of
 * the valuation runs whose date falls in the range, posted (run posted) or unposted (run not yet
 * posted). FinVerse rule: applicable contribution = gross − discount + loading − commission −
 * claims; payable = (applicable − retakaful) × participants' share %, less tax, when positive.
 */
@Component
public class TakafulSurplusReport implements ReportDefinition {

  private static final String FROM = "fromDate";
  private static final String TO = "toDate";
  private static final String STATUS = "posting";
  private static final String POSTED = "POSTED";
  private static final String UNPOSTED = "UNPOSTED";

  private final ReserveReportSupport support;
  private final ValuationRunService runs;

  /**
   * Creates the report.
   *
   * @param support report support
   * @param runs valuation runs
   */
  public TakafulSurplusReport(ReserveReportSupport support, ValuationRunService runs) {
    this.support = support;
    this.runs = runs;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "PGIBR074",
        "Surplus / Mudharabah Payment",
        ReportCategory.ACTUARIAL,
        "Takaful surplus payable to participants of expired policies",
        List.of(
            ParameterSpec.required(ReserveReportSupport.COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.required(FROM, "Approval Date From", ParameterType.DATE)
                .withDefault("YEAR_START"),
            ParameterSpec.required(TO, "Approval Date To", ParameterType.DATE).withDefault("TODAY"),
            ParameterSpec.optional(ReserveReportSupport.BRANCH, "Branch", ParameterType.BRANCH),
            ParameterSpec.select(
                STATUS, "Posted / Unposted", List.of("ALL", POSTED, UNPOSTED), "ALL")),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(ReserveReportSupport.COMPANY);
    LocalDate from = p.date(FROM);
    LocalDate to = p.date(TO);
    String status = p.text(STATUS);
    Map<Long, String> branches = support.branchCodes(companyId);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (ValuationRun run : runs.list(companyId)) {
      if (run.getStatus() == RunStatus.CANCELLED
          || run.getValuationDate().isBefore(from)
          || run.getValuationDate().isAfter(to)
          || !statusMatches(status, run)) {
        continue;
      }
      for (TakafulItem i : runs.takaful(run.getId())) {
        if (ReserveReportSupport.matches(p, i.key())) {
          rows.add(row(i, run, branches));
        }
      }
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("policy", "Policy"),
            ReportColumn.text("insured", "Insured"),
            ReportColumn.text("product", "Product"),
            ReportColumn.date("expiry", "Expiry"),
            ReportColumn.text("run", "Run"),
            ReportColumn.amount("gross", "Gross"),
            ReportColumn.amount("discount", "Discount"),
            ReportColumn.amount("loading", "Loading"),
            ReportColumn.amount("commission", "Commission"),
            ReportColumn.amount("claims", "Claims"),
            ReportColumn.amount("applicable", "Applicable Contribution"),
            ReportColumn.amount("retakaful", "Retakaful"),
            ReportColumn.amount("tax", "Tax"),
            ReportColumn.amount("payable", "Payable"))
        .groupBy(K_BRANCH, "Branch")
        .rows(rows)
        .note(
            "Applicable = gross - discount + loading - commission - claims (FinVerse rule);"
                + " payable = (applicable - retakaful) x participants' share, less tax.")
        .build();
  }

  private static boolean statusMatches(String status, ValuationRun run) {
    return switch (status) {
      case POSTED -> run.isPosted();
      case UNPOSTED -> !run.isPosted();
      default -> true;
    };
  }

  private static Map<String, Object> row(
      TakafulItem i, ValuationRun run, Map<Long, String> branches) {
    Map<String, Object> row = new LinkedHashMap<>();
    ReserveReportSupport.putKey(row, i.key(), branches);
    row.put("policy", i.policyNo());
    row.put("insured", i.insuredName());
    row.put("expiry", i.expiryDate());
    row.put("run", run.getPeriodName() + " " + run.getStatus());
    row.put("gross", i.gross());
    row.put("discount", i.discount());
    row.put("loading", i.loading());
    row.put("commission", i.commission());
    row.put("claims", i.claims());
    row.put("applicable", i.applicable());
    row.put("retakaful", i.retakaful());
    row.put("tax", i.tax());
    row.put("payable", i.payable());
    return row;
  }
}
