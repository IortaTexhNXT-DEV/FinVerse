package com.iortatechnxt.brokerverse.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.render.DocxReportRenderer;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.report.render.PrintOptions;
import com.iortatechnxt.brokerverse.report.render.ReportContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

/** The Word export: a valid document in the layout of the PDF (client requirement 16). */
class DocxReportRendererTest {

  private static final ReportContext CONTEXT =
      new ReportContext(
          "BDO Insurance Brokers", "tester", Instant.parse("2026-09-24T01:00:00Z"), "Footer note");

  private static ReportResult result(int extraColumns) {
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.text("name", "Name"));
    columns.add(ReportColumn.amount("amount", "Amount"));
    for (int i = 0; i < extraColumns; i++) {
      columns.add(ReportColumn.text("x" + i, "Extra " + i));
    }
    return new ReportResult(
        "T-DOCX",
        "Schedule of Cash",
        List.of("As of : 2026-09-24"),
        columns,
        List.of(
            new ReportRow(RowKind.GROUP_HEADER, 0, "Branch A", Map.of()),
            ReportRow.detail(Map.of("name", "Petty cash", "amount", new BigDecimal("1234.5"))),
            ReportRow.detail(Map.of("name", "Cash in bank", "amount", new BigDecimal("-10"))),
            ReportRow.detail(Map.of("name", "Cash on hand", "amount", new BigDecimal("5"))),
            new ReportRow(
                RowKind.SUBTOTAL, 1, "Total Branch A", Map.of("amount", new BigDecimal("1229.5"))),
            new ReportRow(
                RowKind.TOTAL, 0, "Grand Total", Map.of("amount", new BigDecimal("1229.5")))),
        List.of("Amounts in PHP"));
  }

  private static XWPFDocument open(byte[] docx) throws IOException {
    return new XWPFDocument(new ByteArrayInputStream(docx));
  }

  @Test
  void writesTheTitleBlockTableTotalsAndFooter() throws IOException {
    DocxReportRenderer renderer = new DocxReportRenderer();
    assertThat(renderer.format()).isEqualTo(ExportFormat.DOCX);
    try (XWPFDocument doc = open(renderer.render(result(0), CONTEXT))) {
      String text = String.join("\n", doc.getParagraphs().stream().map(p -> p.getText()).toList());
      assertThat(text)
          .contains("Schedule of Cash")
          .contains("Report ID: T-DOCX")
          .contains("User ID: tester")
          .contains("Run Date: 24-09-2026 09:00")
          .contains("Filters: As of : 2026-09-24")
          .contains("Note: Amounts in PHP")
          .contains("*** End of Report ***");

      assertThat(doc.getTables()).hasSize(1);
      XWPFTable table = doc.getTables().get(0);
      XWPFTableRow head = table.getRow(0);
      assertThat(head.isRepeatHeader()).isTrue();
      assertThat(head.getCell(1).getText()).isEqualTo("Name");
      assertThat(head.getCell(2).getText()).isEqualTo("Amount");
      assertThat(head.getCell(1).getColor()).isEqualToIgnoringCase("004EA8");
      assertThat(table.getRows()).hasSize(7);
      // Group header spans every column on a Background Blue row.
      assertThat(table.getRow(1).getTableCells()).hasSize(1);
      assertThat(table.getRow(1).getCell(0).getText()).isEqualTo("Branch A");
      assertThat(table.getRow(1).getCell(0).getColor()).isEqualToIgnoringCase("E5F5FF");
      // Details with formatted amounts; every second detail is banded.
      assertThat(table.getRow(2).getCell(2).getText()).isEqualTo("1,234.50");
      assertThat(table.getRow(3).getCell(2).getText()).isEqualTo("(10.00)");
      assertThat(table.getRow(2).getCell(1).getColor()).isNull();
      assertThat(table.getRow(3).getCell(1).getColor()).isEqualToIgnoringCase("F7FAFD");
      assertThat(table.getRow(5).getCell(0).getText()).isEqualTo("  Total Branch A");
      assertThat(table.getRow(6).getCell(0).getText()).isEqualTo("Grand Total");
      assertThat(table.getCTTbl().getTblPr().getTblW().getType()).isEqualTo(STTblWidth.PCT);

      String footer = doc.getFooterList().get(0).getText();
      assertThat(footer)
          .contains("Confidential  |  Footer note")
          .contains("iNXT BrokerVerse  |  T-DOCX  |  Page");
      assertThat(doc.getFooterList().get(0).getParagraphs().get(1).getCTP().xmlText())
          .contains("NUMPAGES");
      assertThat(doc.getHeaderList().get(0).getText()).contains("BDO Insurance Brokers");
      assertThat(doc.getHeaderList().get(0).getAllPictures()).hasSize(1);
      assertThat(doc.getProperties().getCoreProperties().getTitle()).isEqualTo("Schedule of Cash");
    }
  }

  @Test
  void turnsWideReportsToLandscapeLikeThePdf() throws IOException {
    try (XWPFDocument doc = open(new DocxReportRenderer().render(result(7), CONTEXT))) {
      CTPageSz size = doc.getDocument().getBody().getSectPr().getPgSz();
      assertThat(size.getOrient()).isEqualTo(STPageOrientation.LANDSCAPE);
      assertThat(String.valueOf(size.getW())).isEqualTo("16838");
      assertThat(String.valueOf(size.getH())).isEqualTo("11906");
    }
  }

  @Test
  void honoursPaperOrientationAndNaturalWidth() throws IOException {
    ReportContext ctx = CONTEXT.withPrint(PrintOptions.of("LEGAL", "PORTRAIT", false));
    try (XWPFDocument doc = open(new DocxReportRenderer().render(result(0), ctx))) {
      CTPageSz size = doc.getDocument().getBody().getSectPr().getPgSz();
      assertThat(String.valueOf(size.getW())).isEqualTo("12240");
      assertThat(String.valueOf(size.getH())).isEqualTo("20160");
      assertThat(size.isSetOrient()).isFalse();
      XWPFTable table = doc.getTables().get(0);
      assertThat(table.getCTTbl().getTblPr().getTblW().getType()).isEqualTo(STTblWidth.DXA);
      // Natural width: 1.6 + 2 + 1.2 weights of 40 points.
      assertThat(String.valueOf(table.getCTTbl().getTblPr().getTblW().getW())).isEqualTo("3840");
    }
  }

  @Test
  void blankFooterStillCarriesTheClassification() throws IOException {
    ReportContext ctx = new ReportContext("Co", "tester", Instant.now(), null);
    try (XWPFDocument doc = open(new DocxReportRenderer().render(result(0), ctx))) {
      assertThat(doc.getFooterList().get(0).getText()).startsWith("Confidential");
    }
  }
}
