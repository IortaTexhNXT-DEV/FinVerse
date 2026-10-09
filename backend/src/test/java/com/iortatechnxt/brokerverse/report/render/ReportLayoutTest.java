package com.iortatechnxt.brokerverse.report.render;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.common.excel.SheetColumnWidths;
import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** Where printed reports put group and total labels, and how wide their columns are. */
class ReportLayoutTest {

  private static final List<ReportColumn> COLUMNS =
      List.of(
          ReportColumn.text("arn", "ARN"),
          ReportColumn.date("since", "In Stage Since"),
          ReportColumn.text("handler", "Collection Handler"),
          ReportColumn.amount("premium", "Outstanding Premium"));

  private static ReportRow detail(String arn, String premium) {
    Map<String, Object> cells = new LinkedHashMap<>();
    cells.put("arn", arn);
    cells.put("since", LocalDate.of(2026, 10, 2));
    cells.put("handler", "Bautista, Carlo");
    cells.put("premium", new BigDecimal(premium));
    return ReportRow.detail(cells);
  }

  /** A report grouped by stage whose totals leave the first columns empty. */
  private static ReportResult grouped() {
    return new ReportResult(
        "T-GRP",
        "Grouped",
        List.of(),
        COLUMNS,
        List.of(
            new ReportRow(RowKind.GROUP_HEADER, 0, "Stage : Draft", Map.of()),
            detail("ARN-2026-000001", "17027.86"),
            detail("ARN-2026-000002", "22268.75"),
            new ReportRow(
                RowKind.SUBTOTAL,
                0,
                "Total Stage : Draft (2 records)",
                Map.of("premium", new BigDecimal("39296.61"))),
            new ReportRow(
                RowKind.TOTAL,
                0,
                "Grand Total (2 records)",
                Map.of("premium", new BigDecimal("39296.61")))),
        List.of());
  }

  @Test
  void aTotalThatLeavesTheFirstColumnsEmptyNeedsNoLabelColumn() {
    ReportResult result = grouped();
    assertThat(LabelLayout.needsLabelColumn(result)).isFalse();
    assertThat(LabelLayout.leadingEmpty(COLUMNS, result.rows().get(3).cells())).isEqualTo(3);
  }

  @Test
  void aTotalWithAValueInTheFirstColumnNeedsTheLabelColumn() {
    ReportResult result =
        new ReportResult(
            "T-LBL",
            "Labelled",
            List.of(),
            List.of(ReportColumn.count("n", "Count")),
            List.of(new ReportRow(RowKind.TOTAL, 0, "Grand Total", Map.of("n", 3))),
            List.of());
    assertThat(LabelLayout.needsLabelColumn(result)).isTrue();
  }

  @Test
  void thePdfPrintsTheTotalLabelsAndWholeHeadingWords() throws IOException {
    byte[] pdf =
        new PdfReportRenderer()
            .render(grouped(), new ReportContext("Company", "Tester", Instant.now(), ""));
    try (PdfReader reader = new PdfReader(pdf)) {
      String page = new PdfTextExtractor(reader).getTextFromPage(1);
      assertThat(page)
          .contains("Total Stage : Draft (2 records)")
          .contains("Grand Total (2 records)")
          .contains("Outstanding")
          .doesNotContain("Outstandin\n");
    }
  }

  @Test
  void excelLeavesOutTheEmptyLabelColumnAndSizesColumnsFromTheirContent() throws IOException {
    byte[] xlsx =
        new XlsxReportRenderer()
            .render(grouped(), new ReportContext("Company", "Tester", Instant.now(), ""));
    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      Sheet sheet = wb.getSheetAt(0);
      Row head = header(sheet);
      assertThat(head.getCell(0).getStringCellValue()).isEqualTo("ARN");
      DataFormatter shown = new DataFormatter();
      List<String> firstColumn = new java.util.ArrayList<>();
      for (int r = head.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row != null && row.getCell(0) != null) {
          firstColumn.add(shown.formatCellValue(row.getCell(0)));
        }
      }
      assertThat(firstColumn)
          .contains("Stage : Draft", "ARN-2026-000001", "Total Stage : Draft (2 records)");
      Row detail = sheet.getRow(head.getRowNum() + 2);
      assertThat(detail.getCell(2).getCellStyle().getIndention())
          .isEqualTo(SheetColumnWidths.TEXT_INDENT);
      // The date column is wider than "02-Oct-2026" by the margin, so it never touches the
      // handler's name next to it.
      assertThat(sheet.getColumnWidth(1))
          .isGreaterThanOrEqualTo(("02-Oct-2026".length() + SheetColumnWidths.MARGIN) * 256);
      assertThat(sheet.getColumnWidth(2))
          .isGreaterThanOrEqualTo(("Collection Handler".length() + SheetColumnWidths.MARGIN) * 256);
    }
  }

  @Test
  void numberColumnsOfWholeDaysPrintWithoutDecimals() {
    assertThat(CellFormatter.format(30, ColumnType.NUMBER)).isEqualTo("30");
  }

  private static Row header(Sheet sheet) {
    for (Row row : sheet) {
      if (row.getCell(0) != null
          && "ARN".equals(new DataFormatter().formatCellValue(row.getCell(0)))) {
        return row;
      }
    }
    throw new AssertionError("no heading row");
  }
}
