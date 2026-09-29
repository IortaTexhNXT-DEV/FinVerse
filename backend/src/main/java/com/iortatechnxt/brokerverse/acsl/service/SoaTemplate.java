package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.acsl.domain.SoaLayout;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Kind;
import com.iortatechnxt.brokerverse.common.excel.GuidedSheet;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplate;
import java.util.ArrayList;
import java.util.List;

/**
 * The guided Excel template of an insurer statement of account in its SOA layout (the insurer's, or
 * the standard ACSL layout): the columns the reconciliation reads, with their guide.
 */
final class SoaTemplate {

  private static final String AMOUNT_FORMAT =
      "Amount without thousands separators, e.g. 12500.00; a negative with a minus sign or in"
          + " brackets";

  private SoaTemplate() {}

  /**
   * The template of a layout.
   *
   * @param layout layout
   * @param maxRows largest number of rows of an upload
   * @return template
   */
  static GuidedTemplate of(SoaLayout layout, int maxRows) {
    List<GuideColumn> columns = new ArrayList<>();
    columns.add(
        GuideColumn.of(layout.getInvoiceHeader(), Kind.TEXT, "Invoice number of the line")
            .when("no policy number is given")
            .allowed("Invoice number of BIBS")
            .example("BI-HO-2026-000001"));
    add(
        columns,
        layout.getPolicyHeader(),
        GuideColumn.of("", Kind.TEXT, "Policy number of the line")
            .when("no invoice number is given")
            .example("MC-2026-000123"));
    add(
        columns,
        layout.getAssuredHeader(),
        GuideColumn.of("", Kind.TEXT, "Name of the assured").example("Juan Dela Cruz"));
    add(
        columns,
        layout.getInceptionHeader(),
        GuideColumn.of("", Kind.DATE, "Start of the policy period").example("2026-01-15"));
    add(
        columns,
        layout.getExpiryHeader(),
        GuideColumn.of("", Kind.DATE, "End of the policy period").example("2027-01-15"));
    add(
        columns,
        layout.getGrossHeader(),
        GuideColumn.of("", Kind.AMOUNT, "Gross premium of the line")
            .format(AMOUNT_FORMAT)
            .example("12500.00"));
    columns.add(
        GuideColumn.of(layout.getBalanceHeader(), Kind.AMOUNT, "Balance the insurer shows")
            .mandatory()
            .format(AMOUNT_FORMAT)
            .example("12500.00"));
    add(
        columns,
        layout.getPaidHeader(),
        GuideColumn.of("", Kind.AMOUNT, "Amount paid").format(AMOUNT_FORMAT).example("0.00"));
    return new GuidedTemplate(
        "Insurer statement of account – " + layout.getName(),
        "Loads the insurer's statement of account and reconciles every line with the books.",
        "Accounting officers, from the statement the insurer sends",
        "ACSL > Insurer SOA Reconciliation, button Upload SOA: give the insurer and the covered"
            + " period, choose this file; the reconciliation runs at once.",
        List.of(
            "At most " + maxRows + " rows per file; one insurer and one covered period per file.",
            "The same file cannot be uploaded twice for the insurer.",
            "Dates as Excel dates, dd-MMM-yyyy or MM/dd/yyyy; amounts as Excel numbers or without"
                + " thousands separators.",
            "Keep the header texts: they are the column names of the layout "
                + layout.getName()
                + "."),
        List.of(
            GuidedSheet.of(
                "SOA", "Statement lines", "One row per line of the statement.", columns)),
        maxRows);
  }

  private static void add(List<GuideColumn> columns, String header, GuideColumn guide) {
    if (header != null && !header.isBlank()) {
      columns.add(
          new GuideColumn(
              header,
              guide.need(),
              guide.condition(),
              guide.kind(),
              guide.format(),
              guide.choices(),
              guide.allowed(),
              guide.whatToEnter(),
              guide.note(),
              guide.example(),
              guide.width()));
    }
  }
}
