package com.iortatechnxt.finverse.payables.report;

import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Engine of the PDC-issued registers (FPD004, FPD005, FR2584, FR2585): cheques issued during a
 * period, or outstanding (issued, not presented, cancelled or replaced) as of a date, grouped by
 * paying bank, or by division → department → bank for the "DDB" variants. FC amounts are shown with
 * their base currency value at the payment rate (R-FX).
 */
public abstract class AbstractPdcIssuedReport implements ReportDefinition {

  private static final String DUE_WITHIN = "dueWithinDays";
  private static final LocalDate EARLIEST = LocalDate.of(2000, 1, 1);
  private static final LocalDate LATEST = LocalDate.of(2999, 12, 31);
  private static final String BANK = "bank";
  private static final String DUE_DATE = "dueDate";
  private static final String DIVISION = "division";
  private static final String DEPARTMENT = "department";

  private final PdcRegisterQuery query;
  private final Variant variant;

  /**
   * Creates a register variant.
   *
   * @param query register read model
   * @param variant layout
   */
  protected AbstractPdcIssuedReport(PdcRegisterQuery query, Variant variant) {
    this.query = query;
    this.variant = variant;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(GlReportSupport.companyParam());
    if (variant.outstanding()) {
      params.add(GlReportSupport.asOfParam());
      params.add(
          ParameterSpec.optional(
              DUE_WITHIN, "Cheque date within (days after as-of)", ParameterType.NUMBER));
    } else {
      params.add(GlReportSupport.fromParam());
      params.add(GlReportSupport.toParam());
    }
    if (variant.byDivision()) {
      params.addAll(ReportParams.divisionDepartmentBank());
    }
    return new ReportMetadata(
        variant.code(),
        variant.title(),
        ReportCategory.RECEIVABLES_PAYABLES,
        variant.description(),
        params,
        Permission.REPORT_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> s : query.cheques(filter(p))) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put(DIVISION, s.get(DIVISION));
      row.put(DEPARTMENT, s.get(DEPARTMENT) == null ? "(none)" : s.get(DEPARTMENT));
      row.put(BANK, s.get("bank_code") + " " + s.get("bank_name"));
      row.put(DUE_DATE, s.get("cheque_date"));
      row.put("chequeNo", s.get("cheque_no"));
      row.put("mainAccount", s.get("main_account"));
      row.put("subAccount", s.get("party_code"));
      row.put("accountName", s.get("payee_name"));
      row.put("currency", s.get("currency"));
      row.put("amount", s.get("amount"));
      row.put("lcAmount", s.get("base_amount"));
      row.put("issueNo", s.get("voucher_no"));
      row.put("issueDate", s.get("issue_date"));
      row.put("status", s.get("status"));
      rows.add(row);
    }
    TabularReportBuilder builder =
        TabularReportBuilder.of(p)
            .columns(
                ReportColumn.date(DUE_DATE, "Due Date"),
                ReportColumn.text("chequeNo", "Cheque No."),
                ReportColumn.text("mainAccount", "Issued to Main A/c"),
                ReportColumn.text("subAccount", "Sub A/c"),
                ReportColumn.text("accountName", "Account Name"),
                ReportColumn.text("currency", "Currency"),
                ReportColumn.amountNoTotal("amount", "Cheque Amount"),
                ReportColumn.amount("lcAmount", "LC Amount"),
                ReportColumn.text("issueNo", "Issue No."),
                ReportColumn.date("issueDate", "Date"),
                ReportColumn.text("status", "Status"));
    if (variant.byDivision()) {
      builder.groupBy(DIVISION, "Division").groupBy(DEPARTMENT, "Department");
    }
    return builder.groupBy(BANK, "Bank").presorted().rows(sortRows(rows)).build();
  }

  private List<Map<String, Object>> sortRows(List<Map<String, Object>> rows) {
    if (variant.byDivision()) {
      return rows;
    }
    List<Map<String, Object>> sorted = new ArrayList<>(rows);
    sorted.sort(
        (a, b) -> {
          int bank = String.valueOf(a.get(BANK)).compareTo(String.valueOf(b.get(BANK)));
          return bank != 0
              ? bank
              : ((LocalDate) a.get(DUE_DATE)).compareTo((LocalDate) b.get(DUE_DATE));
        });
    return sorted;
  }

  private PdcRegisterQuery.Filter filter(ReportParameters p) {
    boolean outstanding = variant.outstanding();
    LocalDate asOf = outstanding ? p.date(GlReportSupport.AS_OF) : null;
    LocalDate dueBy =
        outstanding
            ? p.optionalDecimal(DUE_WITHIN).map(d -> asOf.plusDays(d.longValue())).orElse(LATEST)
            : null;
    return new PdcRegisterQuery.Filter(
        p.longValue(GlReportSupport.COMPANY),
        outstanding ? EARLIEST : p.date(GlReportSupport.FROM),
        outstanding ? asOf : p.date(GlReportSupport.TO),
        outstanding,
        asOf,
        dueBy,
        ReportParams.text(p, ReportParams.DIVISION_FROM),
        ReportParams.text(p, ReportParams.DIVISION_TO),
        ReportParams.text(p, ReportParams.DEPARTMENT_FROM),
        ReportParams.text(p, ReportParams.DEPARTMENT_TO),
        ReportParams.text(p, ReportParams.BANK_FROM),
        ReportParams.text(p, ReportParams.BANK_TO));
  }

  /**
   * Report variant.
   *
   * @param code report code
   * @param title title
   * @param description description
   * @param outstanding as-of (outstanding) register instead of a period register
   * @param byDivision division / department / bank grouping and ranges
   */
  protected record Variant(
      String code, String title, String description, boolean outstanding, boolean byDivision) {}
}
