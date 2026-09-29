package com.iortatechnxt.brokerverse.journal.service;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Choice;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Kind;
import com.iortatechnxt.brokerverse.common.excel.GuidedSheet;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplate;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplateWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Downloadable journal upload templates with two example vouchers in the head-office branch and the
 * base currency of the company: the guided Excel template (the template users fill in) and the
 * plain CSV layout (header row, for extracts of other systems).
 */
public final class JournalUploadTemplate {

  private JournalUploadTemplate() {}

  /**
   * Values of the example vouchers.
   *
   * @param valueDate value date
   * @param branchCode branch (the head office of the company), may be blank
   * @param currency currency (the base currency of the company), may be blank
   */
  public record Example(LocalDate valueDate, String branchCode, String currency) {

    /** Blank values instead of null. */
    public Example {
      branchCode = branchCode == null ? "" : branchCode;
      currency = currency == null ? "" : currency;
    }
  }

  /**
   * Example rows in template column order.
   *
   * @param example values of the examples
   * @return rows (without header)
   */
  static List<List<String>> sampleRows(Example example) {
    String date = example.valueDate().toString();
    return List.of(
        List.of(
            "V1",
            example.branchCode(),
            "MANUAL",
            date,
            example.currency(),
            "Office rent for the month",
            "INV-2026-001",
            "5603",
            "15000.00",
            "",
            "",
            "",
            "FIN",
            "",
            "",
            "",
            "Rent - head office"),
        List.of("V1", "", "", "", "", "", "", "1111", "", "15000.00", "", "", "", "", "", "", ""),
        List.of(
            "V2",
            example.branchCode(),
            "ACCRUAL",
            date,
            example.currency(),
            "Accrued professional fees",
            "",
            "5605",
            "8000.00",
            "",
            "",
            "",
            "FIN",
            "",
            "",
            "",
            ""),
        List.of("V2", "", "", "", "", "", "", "2502", "", "8000.00", "", "", "", "", "", "", ""));
  }

  /**
   * CSV template.
   *
   * @param example values of the examples
   * @return UTF-8 CSV bytes
   */
  public static byte[] csv(Example example) {
    StringBuilder text = new StringBuilder(String.join(",", UploadLine.ALL_COLUMNS)).append("\r\n");
    for (List<String> row : sampleRows(example)) {
      text.append(row.stream().map(JournalUploadTemplate::quote).collect(Collectors.joining(",")))
          .append("\r\n");
    }
    return text.toString().getBytes(StandardCharsets.UTF_8);
  }

  /**
   * XLSX template: the guided sheet (title block, column guide above the header, the two example
   * vouchers marked to overwrite or delete, drop-downs and cell checks).
   *
   * @param example values of the examples
   * @return workbook bytes
   */
  public static byte[] xlsx(Example example) {
    return GuidedTemplateWriter.write(guided(example));
  }

  /**
   * The guided template of the journal upload.
   *
   * @param example values of the examples
   * @return template
   */
  static GuidedTemplate guided(Example example) {
    GuidedSheet sheet =
        new GuidedSheet(
            "Journals",
            "Journal lines",
            "One row per journal line; the rows with the same voucher_key form one voucher.",
            columns(),
            sampleRows(example));
    return new GuidedTemplate(
        "Journal upload",
        "Creates many manual, adjustment or accrual journals at once as draft journals for"
            + " approval.",
        "Accountants of the finance team",
        "General Ledger > Journal Upload: download this template, fill it in, upload it with"
            + " Validate (nothing is created), then with Import: every valid voucher becomes a"
            + " draft journal to submit and approve.",
        List.of(
            "At most 5,000 rows and 5 MB per file, for the company chosen on screen.",
            "Debits equal credits in each voucher and currency; a voucher has at least two lines.",
            "The voucher columns (branch_code to reference) are read from the first row of the"
                + " voucher; later rows leave them blank or repeat the same value.",
            "Dates as dd-MMM-yyyy (e.g. 15-Jan-2026) or as Excel dates; amounts without thousands"
                + " separators, or as Excel numbers.",
            "Keep the header texts; the order of the columns does not matter."),
        List.of(sheet),
        JournalUploadService.MAX_ROWS);
  }

  private static List<GuideColumn> columns() {
    String firstRow = "on the first row of each voucher";
    return List.of(
        GuideColumn.of(
                UploadLine.VOUCHER_KEY,
                Kind.TEXT,
                "Any text grouping the lines of one voucher, e.g. V1; used only inside the file")
            .mandatory(),
        GuideColumn.of(UploadLine.BRANCH_CODE, Kind.TEXT, "Branch of the voucher")
            .when(firstRow)
            .allowed("Code of an existing branch"),
        GuideColumn.of(UploadLine.JOURNAL_TYPE, Kind.TEXT, "Type of journal; blank for MANUAL")
            .choices(
                List.of(
                    new Choice("MANUAL", "Manual journal"),
                    new Choice("ADJUSTMENT", "Adjustment"),
                    new Choice("ACCRUAL", "Accrual"))),
        GuideColumn.of(UploadLine.VALUE_DATE, Kind.DATE, "Accounting date of the voucher")
            .when(firstRow),
        GuideColumn.of(UploadLine.CURRENCY, Kind.TEXT, "Currency of the voucher")
            .when(firstRow)
            .format("ISO currency code, 3 letters"),
        GuideColumn.of(UploadLine.NARRATION, Kind.TEXT, "Narration of the voucher")
            .when(firstRow)
            .format("Text, at most 500 characters"),
        GuideColumn.of(UploadLine.REFERENCE, Kind.TEXT, "External reference of the voucher")
            .format("Text, at most 60 characters"),
        GuideColumn.of(UploadLine.ACCOUNT_CODE, Kind.TEXT, "GL account of the line")
            .mandatory()
            .allowed("Code of an existing postable GL account"),
        GuideColumn.of(UploadLine.DEBIT, Kind.AMOUNT, "Debit amount of the line")
            .when("the line is a debit (fill debit or credit, not both)"),
        GuideColumn.of(UploadLine.CREDIT, Kind.AMOUNT, "Credit amount of the line")
            .when("the line is a credit (fill debit or credit, not both)"),
        GuideColumn.of("line_currency", Kind.TEXT, "Currency of the line when it differs")
            .format("ISO currency code, 3 letters"),
        GuideColumn.of(
            "exchange_rate",
            Kind.NUMBER,
            "Exchange rate; blank for the spot rate of the value date"),
        GuideColumn.of("cost_center", Kind.TEXT, "Cost centre of the line")
            .when("the account requires a cost centre")
            .allowed("Code of an existing cost centre"),
        GuideColumn.of("business_line", Kind.TEXT, "Line of business of the line")
            .allowed("Code of an existing line of business"),
        GuideColumn.of("party_code", Kind.TEXT, "Sub-ledger party of the line")
            .when("the account is a control account")
            .allowed("Code of an existing party"),
        GuideColumn.of("line_reference", Kind.TEXT, "Reference of the line")
            .format("Text, at most 60 characters"),
        GuideColumn.of("line_narration", Kind.TEXT, "Narration of the line")
            .format("Text, at most 250 characters"));
  }

  private static String quote(String value) {
    return value.contains(",") || value.contains("\"")
        ? "\"" + value.replace("\"", "\"\"") + "\""
        : value;
  }
}
