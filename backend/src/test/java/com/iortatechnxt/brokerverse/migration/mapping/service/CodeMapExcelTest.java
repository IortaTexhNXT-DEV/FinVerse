package com.iortatechnxt.brokerverse.migration.mapping.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.bulk.service.BulkFileReader;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapEntry;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapEntry.EntryData;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapVersion;
import com.iortatechnxt.brokerverse.migration.mapping.domain.EntryAction;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class CodeMapExcelTest {

  private final CodeMapExcel excel = new CodeMapExcel(new BulkFileReader());

  @Test
  void theExportIsAGuidedSheetThatIsImportedAsItIs() throws IOException {
    EntryData data =
        new EntryData("QPS", "PH", "Filipino", null, null, EntryAction.MAP, "FILIPINO", null);
    byte[] file =
        excel.export(
            new CodeMapVersion("LOV:NATIONALITY", 2, "second"),
            List.of(new CodeMapEntry(1L, data)));
    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(file))) {
      var sheet = wb.getSheetAt(0);
      assertThat(sheet.getRow(0).getCell(0).getStringCellValue())
          .isEqualTo("Code map LOV:NATIONALITY");
      assertThat(wb.getSheet("Lists")).isNotNull();
    }
    List<EntryData> read = excel.read("map.xlsx", file);
    assertThat(read)
        .singleElement()
        .satisfies(
            e -> {
              assertThat(e.legacyCode()).isEqualTo("PH");
              assertThat(e.action()).isEqualTo(EntryAction.MAP);
              assertThat(e.targetCode()).isEqualTo("FILIPINO");
            });
  }
}
