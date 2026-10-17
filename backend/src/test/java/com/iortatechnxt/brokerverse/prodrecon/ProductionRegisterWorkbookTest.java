package com.iortatechnxt.brokerverse.prodrecon;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.common.excel.SheetColumnWidths;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtractLine;
import com.iortatechnxt.brokerverse.prodrecon.service.ProductionRegisterWorkbook;
import com.iortatechnxt.brokerverse.prodrecon.service.RegisterLayout;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** The production register sent to an insurer reads as on the screens. */
class ProductionRegisterWorkbookTest {

  private static ReconExtractLine line() throws ReflectiveOperationException {
    var constructor = ReconExtractLine.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    ReconExtractLine l = constructor.newInstance();
    ReflectionTestUtils.setField(l, "invoiceNo", "BI-HO-2026-000002");
    ReflectionTestUtils.setField(l, "kind", "INVOICE");
    ReflectionTestUtils.setField(l, "remittanceStatus", "WITH_OUTSTANDING_BALANCE");
    ReflectionTestUtils.setField(l, "bookingDate", LocalDate.of(2026, 10, 2));
    ReflectionTestUtils.setField(l, "assuredName", "Garcia, Antonio Luis Dizon");
    ReflectionTestUtils.setField(l, "grossPremium", new BigDecimal("22268.75"));
    return l;
  }

  @Test
  void statusesAreLabelsDatesReadableAndColumnsAsWideAsTheirContent()
      throws ReflectiveOperationException, java.io.IOException {
    byte[] xlsx =
        new ProductionRegisterWorkbook(() -> "secret")
            .write(LocalDate.of(2026, 10, 1), List.of(line()));
    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      Sheet sheet = wb.getSheetAt(0);
      Row head = sheet.getRow(0);
      Row row = sheet.getRow(1);
      DataFormatter shown = new DataFormatter();
      int status = RegisterLayout.HEADERS.indexOf(RegisterLayout.REMITTANCE);
      assertThat(row.getCell(status).getStringCellValue()).isEqualTo("With Outstanding Balance");
      assertThat(row.getCell(status).getCellStyle().getIndention())
          .isEqualTo(SheetColumnWidths.TEXT_INDENT);
      int booking = RegisterLayout.HEADERS.indexOf(RegisterLayout.BOOKING_DATE);
      assertThat(shown.formatCellValue(row.getCell(booking))).isEqualTo("02-Oct-2026");
      for (int c = 0; c < head.getLastCellNum(); c++) {
        String text = row.getCell(c) == null ? "" : shown.formatCellValue(row.getCell(c));
        assertThat(sheet.getColumnWidth(c))
            .as(head.getCell(c).getStringCellValue())
            .isGreaterThanOrEqualTo((text.length() + SheetColumnWidths.MARGIN) * 256);
      }
    }
  }
}
