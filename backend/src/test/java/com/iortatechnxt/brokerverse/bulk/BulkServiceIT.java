package com.iortatechnxt.brokerverse.bulk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJobStatus;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowStatus;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.bulk.service.BulkUpload;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

@IntegrationTest
class BulkServiceIT {

  @Autowired private BulkService bulk;
  @Autowired private TestBulkHandler handler;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private BulkJob upload(String fileName, byte[] content) {
    return as.run(
        "proc",
        () ->
            bulk.upload(
                new BulkUpload(
                    data.company().getId(),
                    TestBulkHandler.CODE,
                    fileName,
                    content,
                    Map.of("product", "MTR08"))));
  }

  @Test
  void csvIsSanitisedValidatedAndCommittedRowByRow() throws IOException {
    String csv =
        "﻿Plate No,Amount,Inception,Fleet,Remarks\n"
            + "abc 1234,\"850,000.00\",2026-10-01,yes,\"said \"\"hello\"\"\"\n"
            + "ABC-1234,100,,,duplicate of row 2\n"
            + "XYZ 999,2000000,,,too high\n"
            + ",5,,,missing plate\n"
            + "DEF 1,12a,31-12-2026,maybe,bad types\n"
            + "FAIL,10,,,fails at commit\n"
            + "GHI 77,10,,N,\n";
    BulkJob job = upload("vehicles.csv", csv.getBytes(StandardCharsets.UTF_8));
    assertThat(job.getStatus()).isEqualTo(BulkJobStatus.VALIDATED);
    assertThat(job.getTotalRows()).isEqualTo(7);
    assertThat(job.getValidRows()).isEqualTo(3);

    var rows = bulk.rows(job.getId(), null, Pageable.ofSize(50)).getContent();
    assertThat(rows).extracting(BulkRowRecord::getRowNo).containsExactly(2, 3, 4, 5, 6, 7, 8);
    assertThat(bulk.values(rows.get(0)))
        .containsEntry("Plate No", "ABC1234")
        .containsEntry("Amount", "850000.00")
        .containsEntry("Fleet", "Y")
        .containsEntry("Remarks", "said \"hello\"");
    assertThat(rows.get(1).getMessages()).contains("Duplicate of row 2");
    assertThat(rows.get(2).getMessages()).contains("Amount above the test limit");
    assertThat(rows.get(3).getMessages()).contains("Plate No is mandatory");
    assertThat(rows.get(4).getMessages())
        .contains("Amount '12a'", "Inception '31-12-2026'", "Fleet 'maybe'");

    BulkJob done = as.run("proc", () -> bulk.commit(job.getId()));
    assertThat(done.getStatus()).isEqualTo(BulkJobStatus.COMPLETED);
    assertThat(done.getCommittedRows()).isEqualTo(2);
    assertThat(done.getFailedRows()).isEqualTo(1);
    assertThat(handler.committed()).contains("ABC1234@MTR08/F", "GHI77@MTR08");
    var failed = bulk.rows(job.getId(), BulkRowStatus.FAILED, Pageable.ofSize(10)).getContent();
    assertThat(failed)
        .singleElement()
        .satisfies(r -> assertThat(r.getMessages()).isEqualTo("Insurer rejected the vehicle"));
    assertThat(bulk.rows(job.getId(), BulkRowStatus.COMMITTED, Pageable.ofSize(10)).getContent())
        .extracting(BulkRowRecord::getResultRef)
        .containsExactly("REF-ABC1234", "REF-GHI77");
    assertThatThrownBy(() -> as.run("proc", () -> bulk.commit(job.getId())))
        .extracting("code")
        .isEqualTo("BULK_JOB_CLOSED");

    try (XSSFWorkbook report =
        new XSSFWorkbook(new ByteArrayInputStream(bulk.report(job.getId())))) {
      assertThat(report.getSheet("Summary").getRow(6).getCell(1).getStringCellValue())
          .isEqualTo("2");
      assertThat(report.getSheet("Rows").getPhysicalNumberOfRows()).isEqualTo(8);
      assertThat(report.getSheet("Rows").getRow(3).getCell(1).getStringCellValue())
          .isEqualTo("INVALID");
    }
  }

  @Test
  void templateRoundTripsAsXlsxAndOdsIsAccepted() throws IOException {
    byte[] template = as.run("proc", () -> bulk.template(TestBulkHandler.CODE));
    BulkJob fromTemplate = upload("template.xlsx", template);
    assertThat(fromTemplate.getValidRows()).isEqualTo(1);
    assertThat(
            bulk.values(
                bulk.rows(fromTemplate.getId(), null, Pageable.ofSize(5)).getContent().get(0)))
        .containsEntry("Plate No", "ABC1234")
        .containsEntry("Amount", "850000.00")
        .containsEntry("Inception", "2026-10-01");
    as.run("proc", () -> bulk.cancel(fromTemplate.getId()));
    assertThat(bulk.job(fromTemplate.getId()).getStatus()).isEqualTo(BulkJobStatus.CANCELLED);

    BulkJob ods = upload("vehicles.ods", ods());
    assertThat(ods.getTotalRows()).isEqualTo(2);
    var odsRows = bulk.rows(ods.getId(), null, Pageable.ofSize(5)).getContent();
    assertThat(bulk.values(odsRows.get(0)))
        .containsEntry("Plate No", "ODS1")
        .containsEntry("Amount", "1500")
        .containsEntry("Inception", "2026-11-30");
    assertThat(bulk.values(odsRows.get(1)))
        .containsEntry("Plate No", "ODS2")
        .doesNotContainKey("Inception");
    assertThat(
            bulk.jobs(data.company().getId(), TestBulkHandler.CODE, Pageable.ofSize(50))
                .getContent())
        .extracting(BulkJob::getId)
        .contains(ods.getId(), fromTemplate.getId());
  }

  @Test
  void rejectsWrongTemplatesTypesAndPermissions() {
    assertThatThrownBy(
            () -> upload("x.csv", "Plate,Amount\nA,1\n".getBytes(StandardCharsets.UTF_8)))
        .extracting("code")
        .isEqualTo("BULK_TEMPLATE_MISMATCH");
    assertThatThrownBy(() -> upload("x.pdf", new byte[] {1}))
        .extracting("code")
        .isEqualTo("BULK_FILE_TYPE");
    assertThatThrownBy(
            () ->
                upload(
                    "x.csv",
                    "Plate No,Amount,Inception,Fleet,Remarks\n".getBytes(StandardCharsets.UTF_8)))
        .extracting("code")
        .isEqualTo("BULK_FILE_EMPTY");
    assertThatThrownBy(
            () ->
                as.run(
                    "auditor",
                    () ->
                        bulk.upload(
                            new BulkUpload(
                                data.company().getId(),
                                TestBulkHandler.CODE,
                                "x.csv",
                                new byte[] {1},
                                null))))
        .extracting("code")
        .isEqualTo("BULK_NOT_PERMITTED");
  }

  private static byte[] ods() throws IOException {
    String t = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
    String o = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";
    String x = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
    String content =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<office:document-content xmlns:office=\""
            + o
            + "\" xmlns:table=\""
            + t
            + "\" xmlns:text=\""
            + x
            + "\">"
            + "<office:body><office:spreadsheet><table:table table:name=\"Data\">"
            + "<table:table-row>"
            + cell("Plate No")
            + cell("Amount")
            + cell("Inception")
            + cell("Fleet")
            + cell("Remarks")
            + "</table:table-row>"
            + "<table:table-row>"
            + cell("ODS1")
            + "<table:table-cell office:value-type=\"float\" office:value=\"1500.00\">"
            + "<text:p>1,500.00</text:p></table:table-cell>"
            + "<table:table-cell office:value-type=\"date\" office:date-value=\"2026-11-30T00:00:00\">"
            + "<text:p>30/11/2026</text:p></table:table-cell>"
            + "<table:table-cell table:number-columns-repeated=\"1020\"/></table:table-row>"
            + "<table:table-row>"
            + cell("ODS2")
            + "<table:table-cell office:value-type=\"float\" office:value=\"2\"><text:p>2</text:p></table:table-cell>"
            + "<table:table-cell table:number-columns-repeated=\"2\"/>"
            + cell("fourth column")
            + "</table:table-row>"
            + "<table:table-row table:number-rows-repeated=\"1048570\">"
            + "<table:table-cell table:number-columns-repeated=\"1024\"/></table:table-row>"
            + "</table:table></office:spreadsheet></office:body></office:document-content>";
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      zip.putNextEntry(new ZipEntry("mimetype"));
      zip.write(
          "application/vnd.oasis.opendocument.spreadsheet".getBytes(StandardCharsets.US_ASCII));
      zip.putNextEntry(new ZipEntry("content.xml"));
      zip.write(content.getBytes(StandardCharsets.UTF_8));
    }
    return out.toByteArray();
  }

  private static String cell(String text) {
    return "<table:table-cell office:value-type=\"string\"><text:p>"
        + text
        + "</text:p></table:table-cell>";
  }
}
