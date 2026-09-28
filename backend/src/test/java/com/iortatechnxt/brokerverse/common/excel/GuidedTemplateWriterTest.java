package com.iortatechnxt.brokerverse.common.excel;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Choice;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Kind;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint.ValidationType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class GuidedTemplateWriterTest {

  private static final List<GuideColumn> COLUMNS =
      List.of(
          GuideColumn.of("Plate No", Kind.TEXT, "Plate number of the vehicle")
              .mandatory()
              .example("ABC1234"),
          GuideColumn.of("Status", Kind.TEXT, "New status")
              .choices(List.of(new Choice("A", "Active"), new Choice("C", "Closed")))
              .when("the vehicle is sold")
              .example("A"),
          GuideColumn.of("Sum Insured", Kind.AMOUNT, "Sum insured").example("1500000.00"),
          GuideColumn.of("Inception", Kind.DATE, "Start of cover")
              .mandatory()
              .example("2026-01-15"),
          GuideColumn.of("Direct", Kind.YES_NO, "Paid directly to the insurer").example("N"),
          GuideColumn.of("Client", Kind.TEXT, "Client of the vehicle")
              .allowed("Code of an existing client"));

  private static GuidedTemplate template() {
    return GuidedTemplate.single(
        "Vehicle upload",
        "Adds vehicles to accounts.",
        "Account officers",
        "Bulk Processing > Bulk Uploads, tile Vehicle upload",
        List.of("At most 5000 rows.", "One company per file."),
        GuidedSheet.of("Vehicles", "Vehicles", "One row per vehicle.", COLUMNS));
  }

  private static XSSFWorkbook open(byte[] bytes) throws IOException {
    return new XSSFWorkbook(new ByteArrayInputStream(bytes));
  }

  private static List<List<String>> table(Sheet sheet) {
    DataFormatter f = new DataFormatter();
    List<List<String>> table = new ArrayList<>();
    for (int r = 0; r <= sheet.getLastRowNum(); r++) {
      Row row = sheet.getRow(r);
      List<String> cells = new ArrayList<>();
      if (row != null) {
        for (int c = 0; c < row.getLastCellNum(); c++) {
          cells.add(row.getCell(c) == null ? "" : f.formatCellValue(row.getCell(c)));
        }
      }
      table.add(cells);
    }
    return table;
  }

  @Test
  void writesTheGuideBandAboveEveryColumnAndMarksMandatoryHeaders() throws IOException {
    try (XSSFWorkbook wb = open(GuidedTemplateWriter.write(template()))) {
      assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("Vehicles");
      assertThat(wb.getSheetAt(1).getSheetName()).isEqualTo("Lists");
      List<List<String>> t = table(wb.getSheetAt(0));
      int header = GuidedTables.headerRow(t, List.of());
      List<String> headers = t.get(header);
      assertThat(headers.subList(1, headers.size()))
          .containsExactly(
              "Plate No *", "Status", "Sum Insured", "Inception *", "Direct", "Client");
      assertThat(GuidedTables.headers(headers))
          .containsExactly(
              "", "Plate No", "Status", "Sum Insured", "Inception", "Direct", "Client");
      List<String> need = t.get(header - 4);
      List<String> format = t.get(header - 3);
      List<String> allowed = t.get(header - 2);
      List<String> what = t.get(header - 1);
      assertThat(need.get(0)).isEqualTo("Mandatory");
      assertThat(need.subList(1, 7))
          .containsExactly("Yes", "Conditional: the vehicle is sold", "No", "Yes", "No", "No");
      assertThat(format.get(4)).startsWith("Date dd-MMM-yyyy");
      assertThat(allowed.get(2)).isEqualTo("A – Active\nC – Closed");
      assertThat(allowed.get(6)).isEqualTo("Code of an existing client");
      assertThat(what.get(0)).isEqualTo("What to enter");
      assertThat(what.get(1)).isEqualTo("Plate number of the vehicle");
      assertThat(t.get(0).get(0)).isEqualTo("Vehicle upload");
      assertThat(t.stream().anyMatch(r -> r.contains("• " + GuidedTemplateWriter.AS_IS_RULE)))
          .isTrue();
    }
  }

  @Test
  void writesOneMarkedExampleRowWithTypedCells() throws IOException {
    try (XSSFWorkbook wb = open(GuidedTemplateWriter.write(template()))) {
      XSSFSheet sheet = wb.getSheetAt(0);
      int header = GuidedTables.headerRow(table(sheet), List.of());
      Row example = sheet.getRow(header + 1);
      assertThat(example.getCell(0).getStringCellValue()).isEqualTo(GuidedTables.EXAMPLE_MARKER);
      assertThat(GuidedTables.isExample(List.of(example.getCell(0).getStringCellValue()))).isTrue();
      assertThat(example.getCell(1).getStringCellValue()).isEqualTo("ABC1234");
      assertThat(example.getCell(3).getNumericCellValue()).isEqualTo(1_500_000d);
      assertThat(example.getCell(4).getCellType()).isEqualTo(CellType.NUMERIC);
      assertThat(example.getCell(4).getLocalDateTimeCellValue().toLocalDate())
          .hasToString("2026-01-15");
      assertThat(wb.getFontAt(example.getCell(1).getCellStyle().getFontIndex()).getItalic())
          .isTrue();
      assertThat(sheet.getRow(header + 2) == null).isTrue();
    }
  }

  @Test
  void addsDropDownsChecksNotesAndFrozenPanes() throws IOException {
    try (XSSFWorkbook wb = open(GuidedTemplateWriter.write(template()))) {
      XSSFSheet sheet = wb.getSheetAt(0);
      int header = GuidedTables.headerRow(table(sheet), List.of());
      List<? extends DataValidation> checks = sheet.getDataValidations();
      assertThat(checks).hasSize(COLUMNS.size());
      DataValidation status =
          checks.stream()
              .filter(v -> v.getRegions().getCellRangeAddress(0).getFirstColumn() == 2)
              .findFirst()
              .orElseThrow();
      assertThat(status.getValidationConstraint().getValidationType())
          .isEqualTo(ValidationType.LIST);
      assertThat(status.getValidationConstraint().getFormula1()).isEqualTo("Lists!$A$4:$A$5");
      assertThat(status.getRegions().getCellRangeAddress(0).getFirstRow()).isEqualTo(header + 1);
      DataValidation direct =
          checks.stream()
              .filter(v -> v.getRegions().getCellRangeAddress(0).getFirstColumn() == 5)
              .findFirst()
              .orElseThrow();
      assertThat(direct.getValidationConstraint().getExplicitListValues())
          .containsExactly("Y", "N");
      assertThat(checks.stream().allMatch(DataValidation::getShowPromptBox)).isTrue();
      assertThat(
              sheet
                  .getCellComment(new org.apache.poi.ss.util.CellAddress(header, 2))
                  .getString()
                  .getString())
          .contains("New status", "Conditional: the vehicle is sold", "C – Closed");
      assertThat(sheet.getPaneInformation().getHorizontalSplitPosition())
          .isEqualTo((short) (header + 1));
      assertThat(sheet.getPaneInformation().getVerticalSplitPosition()).isEqualTo((short) 1);
      assertThat(sheet.getPrintSetup().getLandscape()).isTrue();
      Sheet lists = wb.getSheet("Lists");
      assertThat(lists.getRow(3).getCell(0).getStringCellValue()).isEqualTo("A");
      assertThat(lists.getRow(4).getCell(1).getStringCellValue()).isEqualTo("Closed");
    }
  }

  @Test
  void aTemplateOfSeveralSheetsStartsWithTheOrderAndLinks() throws IOException {
    GuidedSheet lines =
        GuidedSheet.of(
            "Lines",
            "Lines",
            "One row per line.",
            List.of(GuideColumn.of("Amount", Kind.AMOUNT, "Amount").mandatory()));
    GuidedTemplate two = template().withSheets(List.of(template().sheets().get(0), lines));
    try (XSSFWorkbook wb = open(GuidedTemplateWriter.write(two))) {
      assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo(GuidedTemplateWriter.START_SHEET);
      assertThat(wb.getSheetAt(1).getSheetName()).isEqualTo("Vehicles");
      assertThat(wb.getSheetAt(2).getSheetName()).isEqualTo("Lines");
      List<List<String>> start = table(wb.getSheetAt(0));
      assertThat(start.stream().anyMatch(r -> r.contains("Lines"))).isTrue();
      Row link = wb.getSheetAt(0).getRow(start.size() - 1);
      assertThat(link.getCell(1).getHyperlink().getAddress()).isEqualTo("'Lines'!A1");
      List<List<String>> second = table(wb.getSheetAt(2));
      assertThat(GuidedTables.isGuided(second)).isTrue();
      assertThat(GuidedTables.headers(second.get(GuidedTables.headerRow(second, List.of()))))
          .containsExactly("", "Amount");
    }
  }

  @Test
  void theReaderFindsTheHeaderOfPlainAndGuidedTables() {
    List<List<String>> plain = List.of(List.of("Plate No", "Status"), List.of("ABC", "A"));
    assertThat(GuidedTables.headerRow(plain, List.of("Plate No"))).isZero();
    List<List<String>> movedHeader =
        List.of(List.of("Vehicle upload"), List.of(""), List.of("", "Plate No *", "Status"));
    assertThat(GuidedTables.headerRow(movedHeader, List.of("Plate No", "Status"))).isEqualTo(2);
    assertThat(GuidedTables.isExample(List.of("Example - overwrite or delete", "X"))).isTrue();
    assertThat(GuidedTables.isExample(List.of("Example Corp", "X"))).isFalse();
    assertThat(GuidedTables.header("Plate No *")).isEqualTo("Plate No");
  }
}
