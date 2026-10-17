package com.iortatechnxt.brokerverse.budget.service;

import com.iortatechnxt.brokerverse.budget.domain.BudgetLine;
import com.iortatechnxt.brokerverse.budget.service.BudgetCsvParser.ParsedLine;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Kind;
import com.iortatechnxt.brokerverse.common.excel.GuidedSheet;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplate;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplateWriter;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The guided Excel template of the budget import and the reading of a filled one: one row per
 * account and cost centre with the twelve monthly amounts, or the annual amount spread evenly
 * (remainder in month 12).
 */
public final class BudgetTemplate {

  /** Account column. */
  public static final String ACCOUNT = "account_code";

  /** Cost centre column. */
  public static final String COST_CENTRE = "cost_centre";

  /** Annual amount column. */
  public static final String ANNUAL = "annual";

  private static final int MAX_ERRORS = 20;
  private static final int MONTH_WIDTH = 12;
  private static final String BY_MONTH = "the amounts are given by month";

  private BudgetTemplate() {}

  /**
   * The columns of the template, in order.
   *
   * @return headers
   */
  public static List<String> headers() {
    List<String> headers = new ArrayList<>(List.of(ACCOUNT, COST_CENTRE));
    for (int m = 1; m <= BudgetLine.MONTHS; m++) {
      headers.add(month(m));
    }
    headers.add(ANNUAL);
    return headers;
  }

  /**
   * The guided template.
   *
   * @return xlsx
   */
  public static byte[] xlsx() {
    List<GuideColumn> columns = new ArrayList<>();
    columns.add(
        GuideColumn.of(ACCOUNT, Kind.TEXT, "Income or expense account of the budget line")
            .mandatory()
            .allowed("Code of an existing postable GL account")
            .example("5609"));
    columns.add(
        GuideColumn.of(COST_CENTRE, Kind.TEXT, "Cost centre of the line; blank for none")
            .allowed("Code of an existing cost centre")
            .example("MKT"));
    for (int m = 1; m <= BudgetLine.MONTHS; m++) {
      String name = Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
      columns.add(
          GuideColumn.of(month(m), Kind.AMOUNT, "Budget of " + name)
              .when(BY_MONTH)
              .width(MONTH_WIDTH)
              .example("300000.00"));
    }
    columns.add(
        GuideColumn.of(
                ANNUAL,
                Kind.AMOUNT,
                "Budget of the year, spread evenly over the months (remainder in December)")
            .when("no monthly amount is given"));
    return GuidedTemplateWriter.write(
        GuidedTemplate.single(
            "Budget import",
            "Replaces the lines of a draft budget with the lines of this file.",
            "The budget owners of the finance team",
            "Planning & Closing > Budgets, open the draft budget, box Import Lines: choose this file."
                + " All lines of the draft are replaced.",
            List.of(
                "One row per account and cost centre; income and expense budgets are both"
                    + " positive.",
                "Give either the twelve monthly amounts or the annual amount.",
                "Amounts as Excel numbers or without thousands separators.",
                "Keep the header texts."),
            GuidedSheet.of("Budget lines", "Budget lines", "One row per budget line.", columns)));
  }

  /**
   * The lines of a filled template (or of a plain workbook with the same headers).
   *
   * @param file read file
   * @return lines
   */
  static List<ParsedLine> lines(ParsedFile file) {
    if (!file.headers().contains(ACCOUNT)) {
      throw invalid(List.of("The file must have the column " + ACCOUNT));
    }
    List<ParsedLine> lines = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    for (ParsedFile.RawRow row : file.rows()) {
      if (errors.size() >= MAX_ERRORS) {
        break;
      }
      line(row, lines, errors);
    }
    if (!errors.isEmpty()) {
      throw invalid(errors);
    }
    return lines;
  }

  private static void line(ParsedFile.RawRow row, List<ParsedLine> lines, List<String> errors) {
    String prefix = "Row " + row.rowNo() + ": ";
    Map<String, String> v = row.values();
    String account = v.get(ACCOUNT);
    if (account == null || account.isBlank()) {
      errors.add(prefix + "account code is missing");
      return;
    }
    try {
      lines.add(new ParsedLine(account.strip(), costCentre(v), months(v)));
    } catch (NumberFormatException e) {
      errors.add(prefix + "amounts must be numbers");
    }
  }

  private static String costCentre(Map<String, String> v) {
    String c = v.get(COST_CENTRE);
    return c == null || c.isBlank() ? null : c.strip();
  }

  private static List<BigDecimal> months(Map<String, String> v) {
    List<BigDecimal> months = new ArrayList<>();
    boolean any = false;
    for (int m = 1; m <= BudgetLine.MONTHS; m++) {
      String text = v.get(month(m));
      any |= text != null && !text.isBlank();
      months.add(amount(text));
    }
    return any ? months : BudgetSpread.even(amount(v.get(ANNUAL)));
  }

  private static BigDecimal amount(String text) {
    return text == null || text.isBlank()
        ? BigDecimal.ZERO
        : new BigDecimal(text.strip().replace(",", ""));
  }

  private static String month(int m) {
    return String.format(Locale.ROOT, "m%02d", m);
  }

  private static BusinessRuleException invalid(List<String> errors) {
    return new BusinessRuleException("BUDGET_IMPORT_INVALID", String.join("; ", errors));
  }
}
