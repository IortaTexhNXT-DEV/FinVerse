package com.iortatechnxt.finverse.payables.report;

import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * FIN-PDC-CONF-AUDIT-DDB – PDC Confirmation Audit Trail by Division/Department and Bank (Src
 * FR2586): the confirmation vouchers (issued PDC presented and accounted, Dr PDC clearing / Cr
 * bank) dated in a period, with their journal lines, to reconcile bank debits with PDCs issued.
 */
@Component
public class PdcConfirmationAuditReport implements ReportDefinition {

  private static final String DEBIT = "debit";
  private static final String CREDIT = "credit";
  private static final String DIVISION = "division";
  private static final String DEPARTMENT = "department";

  private final PdcRegisterQuery query;

  /**
   * Creates the report.
   *
   * @param query register read model
   */
  public PdcConfirmationAuditReport(PdcRegisterQuery query) {
    this.query = query;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(GlReportSupport.companyParam());
    params.add(GlReportSupport.fromParam());
    params.add(GlReportSupport.toParam());
    params.addAll(ReportParams.divisionDepartmentBank());
    return new ReportMetadata(
        "FIN-PDC-CONF-AUDIT-DDB",
        "PDC Confirmation Audit Trail by Division/Department and Bank",
        ReportCategory.RECEIVABLES_PAYABLES,
        "Confirmation vouchers of presented PDCs issued, with journal lines (FR2586)",
        params,
        Permission.REPORT_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<Map<String, Object>> source =
        query.confirmations(
            new PdcRegisterQuery.Filter(
                p.longValue(GlReportSupport.COMPANY),
                p.date(GlReportSupport.FROM),
                p.date(GlReportSupport.TO),
                false,
                null,
                null,
                ReportParams.text(p, ReportParams.DIVISION_FROM),
                ReportParams.text(p, ReportParams.DIVISION_TO),
                ReportParams.text(p, ReportParams.DEPARTMENT_FROM),
                ReportParams.text(p, ReportParams.DEPARTMENT_TO),
                ReportParams.text(p, ReportParams.BANK_FROM),
                ReportParams.text(p, ReportParams.BANK_TO)));
    List<Map<String, Object>> rows = new ArrayList<>();
    Set<Object> vouchers = new HashSet<>();
    BigDecimal totalDebit = BigDecimal.ZERO;
    BigDecimal totalCredit = BigDecimal.ZERO;
    for (Map<String, Object> s : source) {
      boolean debit = "DEBIT".equals(s.get("side"));
      BigDecimal base = (BigDecimal) s.get("base_amount");
      rows.add(row(s, debit, base));
      vouchers.add(s.get("batch_no"));
      totalDebit = debit ? totalDebit.add(base) : totalDebit;
      totalCredit = debit ? totalCredit : totalCredit.add(base);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("account", "Main"),
            ReportColumn.text("subAccount", "Sub"),
            ReportColumn.text("accountName", "Account Name / Line Narration"),
            ReportColumn.text("currency", "Currency"),
            ReportColumn.amountNoTotal("fcAmount", "FC Amount"),
            ReportColumn.amount(DEBIT, "LC Amount Dr"),
            ReportColumn.amount(CREDIT, "LC Amount Cr"))
        .groupBy(DIVISION, "Division")
        .groupBy(DEPARTMENT, "Department")
        .groupBy("bank", "Bank")
        .groupBy("voucher", "Voucher")
        .presorted()
        .rows(rows)
        .note(
            "Summary: vouchers "
                + vouchers.size()
                + ", entries "
                + rows.size()
                + ", total debit in base currency "
                + totalDebit.toPlainString()
                + ", total credit in base currency "
                + totalCredit.toPlainString())
        .build();
  }

  private static Map<String, Object> row(Map<String, Object> s, boolean debit, BigDecimal base) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(DIVISION, s.get(DIVISION));
    row.put(DEPARTMENT, s.get(DEPARTMENT) == null ? "(none)" : s.get(DEPARTMENT));
    row.put("bank", s.get("bank_code") + " " + s.get("bank_name"));
    row.put(
        "voucher",
        s.get("batch_no")
            + " | Doc/Bank Date "
            + s.get("value_date")
            + " | Due "
            + s.get("cheque_date")
            + " | Cheque "
            + s.get("cheque_no")
            + " | "
            + s.get("narration"));
    row.put("account", s.get("account_code"));
    row.put("subAccount", s.get("party_code"));
    row.put("accountName", s.get("account_name") + " / " + s.get("line_narration"));
    row.put("currency", s.get("currency"));
    row.put("fcAmount", s.get("amount"));
    row.put(debit ? DEBIT : CREDIT, base);
    return row;
  }
}
