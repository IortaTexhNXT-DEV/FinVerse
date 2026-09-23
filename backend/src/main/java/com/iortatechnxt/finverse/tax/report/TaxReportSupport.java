package com.iortatechnxt.finverse.tax.report;

import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.tax.domain.ReturnLineValues;
import com.iortatechnxt.finverse.tax.domain.TaxPeriod;
import com.iortatechnxt.finverse.tax.service.LedgerControl;
import com.iortatechnxt.finverse.tax.service.TaxDocumentLine;
import com.iortatechnxt.finverse.tax.service.TaxWorksheet;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parameters, columns and row builders shared by the tax and statutory reports. */
final class TaxReportSupport {

  static final String COMPANY = "companyId";
  static final String FROM = "fromDate";
  static final String TO = "toDate";
  static final String YEAR = "year";
  static final String QUARTER = "quarter";
  static final String CODE = "code";
  static final String DESCRIPTION = "description";
  static final String BASE = "base";
  static final String AMOUNT = "amount";
  static final String MONTH = "month";
  static final String PARTY = "party";
  static final String TIN = "tin";
  static final String TAX = "tax";

  private TaxReportSupport() {}

  static ParameterSpec company() {
    return ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY);
  }

  static ParameterSpec from() {
    return ParameterSpec.required(FROM, "From date", ParameterType.DATE).withDefault("YEAR_START");
  }

  static ParameterSpec to() {
    return ParameterSpec.required(TO, "To date", ParameterType.DATE).withDefault("TODAY");
  }

  static ParameterSpec year(Clock clock) {
    return ParameterSpec.required(YEAR, "Year", ParameterType.NUMBER)
        .withDefault(String.valueOf(LocalDate.now(clock).getYear()));
  }

  static ParameterSpec quarter(Clock clock) {
    return ParameterSpec.select(
        QUARTER,
        "Quarter",
        List.of("1", "2", "3", "4"),
        String.valueOf(TaxPeriod.quarterNumber(LocalDate.now(clock))));
  }

  static Long companyId(ReportParameters p) {
    return p.longValue(COMPANY);
  }

  static TaxPeriod period(ReportParameters p) {
    return new TaxPeriod(p.date(FROM), p.date(TO));
  }

  static TaxPeriod quarterPeriod(ReportParameters p) {
    return TaxPeriod.quarter(year(p), Integer.parseInt(p.text(QUARTER)));
  }

  static int year(ReportParameters p) {
    return Integer.parseInt(p.text(YEAR));
  }

  /** Columns of a worksheet summary: line code, description, base, amount. */
  static List<ReportColumn> summaryColumns() {
    return List.of(
        ReportColumn.text(CODE, "Line"),
        ReportColumn.text(DESCRIPTION, "Description"),
        ReportColumn.amountNoTotal(BASE, "Tax base"),
        ReportColumn.amountNoTotal(AMOUNT, "Amount"));
  }

  /** Rows of a worksheet summary. */
  static List<Map<String, Object>> summaryRows(List<ReturnLineValues> lines) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (ReturnLineValues l : lines) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put(CODE, l.code());
      row.put(DESCRIPTION, l.description());
      row.put(BASE, l.base());
      row.put(AMOUNT, l.amount());
      rows.add(row);
    }
    return rows;
  }

  /** Footnotes: ledger reconciliation and worksheet remarks. */
  static List<String> notes(TaxWorksheet w) {
    List<String> notes = new ArrayList<>();
    for (LedgerControl c : w.controls()) {
      notes.add(
          "Ledger control "
              + c.description()
              + " ("
              + c.accountCode()
              + "): documents "
              + c.perDocuments().toPlainString()
              + ", ledger "
              + c.perLedger().toPlainString()
              + ", difference "
              + c.difference().toPlainString());
    }
    notes.addAll(w.notes());
    return notes;
  }

  /** A drill-down row of a premium document (DST, premium tax, LGT, FST registers). */
  static Map<String, Object> premiumRow(TaxDocumentLine d) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(MONTH, YearMonth.from(d.documentDate()).toString());
    row.put("documentNo", d.documentNo());
    row.put("date", d.documentDate());
    row.put(PARTY, d.partyName());
    row.put(TIN, d.tin());
    row.put("lob", d.businessLine());
    row.put(BASE, d.taxableAmount());
    row.put(TAX, d.taxAmount());
    return row;
  }

  /** Columns of {@link #premiumRow}. */
  static List<ReportColumn> premiumColumns(String taxLabel) {
    return List.of(
        ReportColumn.text("documentNo", "Policy / endorsement"),
        ReportColumn.date("date", "Accounting date"),
        ReportColumn.text(PARTY, "Insured / client"),
        ReportColumn.text(TIN, "TIN"),
        ReportColumn.text("lob", "Line of business"),
        ReportColumn.amount(BASE, "Premium"),
        ReportColumn.amount(TAX, taxLabel));
  }
}
