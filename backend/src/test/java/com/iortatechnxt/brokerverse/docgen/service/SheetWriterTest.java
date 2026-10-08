package com.iortatechnxt.brokerverse.docgen.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.common.excel.SheetColumnWidths;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** Billing and placement files have columns as wide as their content. */
class SheetWriterTest {

  @Test
  void aDateOrAmountNeverTouchesTheNextColumn() throws IOException {
    byte[] xlsx =
        SheetWriter.write(
            List.of(
                new SheetSpec(
                    "Billing",
                    List.of("Booking Date", "Borrower", "Premium", "Reference"),
                    List.of(
                        List.of(
                            LocalDate.of(2026, 10, 2),
                            "Bautista, Carmela Reyes",
                            new BigDecimal("14315.88"),
                            "ARN-2026-000008")))));
    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      Sheet sheet = wb.getSheetAt(0);
      assertThat(sheet.getColumnWidth(0))
          .isGreaterThanOrEqualTo(("Booking Date".length() + SheetColumnWidths.MARGIN) * 256);
      assertThat(sheet.getColumnWidth(1))
          .isGreaterThanOrEqualTo(
              ("Bautista, Carmela Reyes".length() + SheetColumnWidths.MARGIN) * 256);
      assertThat(sheet.getColumnWidth(2))
          .isGreaterThanOrEqualTo(("14,315.88".length() + SheetColumnWidths.MARGIN) * 256);
      assertThat(sheet.getRow(0).getCell(0).getCellStyle().getWrapText()).isTrue();
    }
  }
}
