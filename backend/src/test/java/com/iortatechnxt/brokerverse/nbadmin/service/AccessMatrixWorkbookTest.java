package com.iortatechnxt.brokerverse.nbadmin.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.common.excel.SheetColumnWidths;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessMatrixWorkbook.Grid;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessMatrixWorkbook.Header;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** The exported User Access Matrix reads like the other Excel reports. */
class AccessMatrixWorkbookTest {

  private static final List<String> PROFILES =
      List.of("UAM_REQUESTOR", "UAM_APPROVER", "UAM_SECOND_APPROVER", "ISO", "SYSADMIN", "AUDITOR");

  @Test
  void theHeaderBlockFrozenPanesUprightProfileCodesFilterAndWidths() throws IOException {
    Grid grid =
        new Grid(
            "User Access Matrix",
            new Header(
                "BDO Insurance and Reinsurance Brokers, Inc.",
                "User Access Matrix",
                "Run By: Audrey Auditor   Run Date: 08-Oct-2026 10:00",
                "2 permissions x 6 group profiles."),
            List.of("Permission", "Area", "Action class"),
            PROFILES,
            List.of(
                List.of("Enabled users", "", "", 1, 2, 1, 1, 1, 1),
                List.of("ACCESS_REQUEST", "User Access", "CREATE", "Y", "", "", "", "Y", ""),
                List.of("ACCESS_APPROVE", "User Access", "APPROVE", "", "Y", "Y", "", "", "")),
            false);
    byte[] xlsx = AccessMatrixWorkbook.write(List.of(grid));
    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      XSSFSheet sheet = wb.getSheetAt(0);
      assertThat(sheet.getRow(1).getCell(0).getStringCellValue())
          .isEqualTo("BDO Insurance and Reinsurance Brokers, Inc.");
      assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("User Access Matrix");
      assertThat(sheet.getDrawingPatriarch().getShapes()).hasSize(1);
      Row head = headingRow(sheet);
      assertThat(head.getCell(3).getStringCellValue()).isEqualTo("UAM_REQUESTOR");
      assertThat(head.getCell(3).getCellStyle().getRotation()).isEqualTo((short) 90);
      PaneInformation pane = sheet.getPaneInformation();
      assertThat(pane.isFreezePane()).isTrue();
      assertThat(pane.getVerticalSplitLeftColumn()).isEqualTo((short) 3);
      assertThat(pane.getHorizontalSplitTopRow()).isEqualTo((short) (head.getRowNum() + 1));
      assertThat(sheet.getCTWorksheet().getAutoFilter().getRef())
          .startsWith("A" + (head.getRowNum() + 1));
      assertThat(sheet.getColumnWidth(0))
          .isGreaterThanOrEqualTo(("ACCESS_REQUEST".length() + SheetColumnWidths.MARGIN) * 256);
      assertThat(sheet.getRepeatingRows()).isNotNull();
    }
  }

  private static Row headingRow(XSSFSheet sheet) {
    for (Row row : sheet) {
      if (row.getCell(0) != null && "Permission".equals(row.getCell(0).getStringCellValue())) {
        return row;
      }
    }
    throw new AssertionError("no heading row");
  }
}
