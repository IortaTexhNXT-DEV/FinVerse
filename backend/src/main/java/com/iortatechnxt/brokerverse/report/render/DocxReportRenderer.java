package com.iortatechnxt.brokerverse.report.render;

import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import com.iortatechnxt.brokerverse.common.office.BrandedDocx;
import com.iortatechnxt.brokerverse.common.office.BrandedDocx.Page;
import com.iortatechnxt.brokerverse.common.office.BrandedDocx.TextStyle;
import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

/**
 * Word (DOCX) export in the layout of the PDF (client requirement 16; A1-FRBS board schedules): the
 * BDO Insure logo and company in the page header, the title block with report ID, user, run date
 * and filters (BRNB.031), the table with a Header Blue heading row repeated on every page, banded
 * detail rows, group headers, subtotals and a grand total under a gold rule, the notes, "*** End of
 * Report ***" and a footer with the "Confidential" classification, the administrator's footer text
 * and "Page x of y". Paper, orientation and fit to width follow the print options (FRBS 2.4.9)
 * exactly as the PDF does.
 */
@Component
public class DocxReportRenderer implements ReportRenderer {

  private static final double LABEL_WEIGHT = 1.6;
  private static final double TEXT_WEIGHT = 2;
  private static final double NUMBER_WEIGHT = 1.2;
  private static final double NATURAL_TWIPS_PER_WEIGHT = 40.0 * BrandedDocx.TWIPS_PER_POINT;
  // Paper sizes in twips (portrait).
  private static final long A4_WIDTH = 11_906;
  private static final long A4_HEIGHT = 16_838;
  private static final long A3_HEIGHT = 23_811;
  private static final long US_WIDTH = 12_240;
  private static final long LETTER_HEIGHT = 15_840;
  private static final long LEGAL_HEIGHT = 20_160;
  private static final long MARGIN = 28L * BrandedDocx.TWIPS_PER_POINT;
  private static final DateTimeFormatter STAMP =
      DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZoneId.of("Asia/Manila"));

  @Override
  public ExportFormat format() {
    return ExportFormat.DOCX;
  }

  @Override
  public byte[] render(ReportResult result, ReportContext context) {
    PrintOptions print = context.print();
    BrandedDocx docx = new BrandedDocx(page(result, print), result.title());
    docx.pageHeader(context.companyName());
    docx.pageFooter(context.footerText(), "iNXT BrokerVerse  |  " + result.code());
    docx.paragraph(result.title(), TextStyle.TITLE);
    docx.paragraph(
        "Report ID: "
            + result.code()
            + "    User ID: "
            + context.generatedBy()
            + "    Run Date: "
            + STAMP.format(context.generatedAt()),
        TextStyle.META);
    if (!result.parameterEcho().isEmpty()) {
      docx.paragraph("Filters: " + String.join("    ", result.parameterEcho()), TextStyle.META);
    }
    table(docx, result, print.fitToWidth());
    for (String note : result.notes()) {
      docx.paragraph("Note: " + note, TextStyle.META);
    }
    docx.paragraph("*** End of Report ***", TextStyle.META).setAlignment(ParagraphAlignment.CENTER);
    return docx.bytes();
  }

  /**
   * The Word page of the print options: the paper of the PDF, turned for wide reports on AUTO.
   *
   * @param result report
   * @param print options
   * @return page
   */
  static Page page(ReportResult result, PrintOptions print) {
    boolean landscape = print.landscape(result.columns().size());
    return switch (print.paper()) {
      case A4 -> new Page(A4_WIDTH, A4_HEIGHT, landscape, MARGIN);
      case LETTER -> new Page(US_WIDTH, LETTER_HEIGHT, landscape, MARGIN);
      case LEGAL -> new Page(US_WIDTH, LEGAL_HEIGHT, landscape, MARGIN);
      case A3 -> new Page(A4_HEIGHT, A3_HEIGHT, landscape, MARGIN);
    };
  }

  private static void table(BrandedDocx docx, ReportResult result, boolean fitToWidth) {
    List<ReportColumn> cols = result.columns();
    double[] weights = new double[cols.size() + 1];
    weights[0] = LABEL_WEIGHT;
    List<String> headings = new ArrayList<>();
    headings.add("");
    for (int i = 0; i < cols.size(); i++) {
      weights[i + 1] = cols.get(i).type() == ColumnType.TEXT ? TEXT_WEIGHT : NUMBER_WEIGHT;
      headings.add(cols.get(i).label());
    }
    XWPFTable table = docx.table(weights, fitToWidth, NATURAL_TWIPS_PER_WEIGHT);
    BrandedDocx.headingRow(table, headings);
    int details = 0;
    for (ReportRow row : result.rows()) {
      XWPFTableRow x = table.createRow();
      x.setCantSplitRow(true);
      if (row.kind() == RowKind.GROUP_HEADER || row.kind() == RowKind.SECTION) {
        BrandedDocx.cell(
            BrandedDocx.span(x, cols.size() + 1),
            indent(row) + row.label(),
            TextStyle.TABLE_BOLD,
            false,
            BrandAssets.BACKGROUND_BLUE);
      } else {
        boolean banded = row.kind() == RowKind.DETAIL && details % 2 == 1;
        valueRow(x, row, cols, banded ? BrandAssets.ROW_BAND : null);
      }
      if (row.kind() == RowKind.DETAIL) {
        details++;
      }
    }
  }

  private static void valueRow(
      XWPFTableRow x, ReportRow row, List<ReportColumn> cols, String band) {
    boolean emphasis = row.kind() == RowKind.SUBTOTAL || row.kind() == RowKind.TOTAL;
    String fill = emphasis ? BrandAssets.BAND : band;
    TextStyle style = emphasis ? TextStyle.TABLE_BOLD : TextStyle.TABLE;
    String label = row.label() == null ? "" : indent(row) + row.label();
    BrandedDocx.cell(x.getCell(0), label, style, false, fill);
    for (int i = 0; i < cols.size(); i++) {
      ReportColumn c = cols.get(i);
      XWPFTableCell cell = x.getCell(i + 1);
      BrandedDocx.cell(
          cell,
          CellFormatter.format(row.cells().get(c.key()), c.type()),
          style,
          rightAligned(c.type()),
          fill);
      if (row.kind() == RowKind.TOTAL) {
        BrandedDocx.goldTop(cell);
      }
    }
  }

  private static boolean rightAligned(ColumnType type) {
    return type != ColumnType.TEXT && type != ColumnType.DATE;
  }

  private static String indent(ReportRow row) {
    return "  ".repeat(row.level());
  }
}
