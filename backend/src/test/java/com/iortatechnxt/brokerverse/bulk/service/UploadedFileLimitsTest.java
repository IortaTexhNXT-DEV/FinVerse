package com.iortatechnxt.brokerverse.bulk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.office.OfficeFileLimits;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Decompression and size limits of the bulk upload readers: zip bombs in OpenDocument and Excel
 * files, too many archive parts, over-expanded repeated rows, extreme numbers, and the content
 * checks of text files.
 */
class UploadedFileLimitsTest {

  private static final String TABLE_NS = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
  private static final String OFFICE_NS = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";
  private static final int MB = 1024 * 1024;

  private final BulkFileReader reader = new BulkFileReader();

  @Test
  void aSmallOpenDocumentSpreadsheetIsRead() throws IOException {
    byte[] ods =
        ods(
            rows(
                "<table:table-row>"
                    + cell("Code")
                    + cell("Amount")
                    + "</table:table-row>"
                    + "<table:table-row>"
                    + cell("A1")
                    + "<table:table-cell office:value-type=\"float\" office:value=\"1500.50\"/>"
                    + "</table:table-row>"),
            0);

    ParsedFile file = reader.read("accounts.ods", ods);

    assertThat(file.headers()).containsExactly("Code", "Amount");
    assertThat(file.rows())
        .singleElement()
        .satisfies(
            r ->
                assertThat(r.values())
                    .containsEntry("Code", "A1")
                    .containsEntry("Amount", "1500.5"));
  }

  @Test
  void anOpenDocumentZipBombIsRefused() throws IOException {
    // 20 MB of blanks compress to a few kilobytes: far beyond 100 times the file size.
    String padding = " ".repeat(20 * MB);
    byte[] bomb = ods(rows("<table:table-row>" + cell("Code") + "</table:table-row>") + padding, 0);
    assertThat(bomb.length).isLessThan(MB);

    assertThatThrownBy(() -> reader.read("bomb.ods", bomb))
        .isInstanceOfSatisfying(
            BusinessRuleException.class,
            e -> assertThat(e.getCode()).isEqualTo(OfficeFileLimits.LIMIT_CODE))
        .hasMessageContaining("compressed-file bomb");
  }

  @Test
  void anOpenDocumentWithTooManyPartsIsRefused() throws IOException {
    byte[] crowded = ods(rows("<table:table-row>" + cell("Code") + "</table:table-row>"), 1001);

    assertThatThrownBy(() -> reader.read("crowded.ods", crowded))
        .isInstanceOfSatisfying(
            BusinessRuleException.class,
            e -> assertThat(e.getCode()).isEqualTo(OfficeFileLimits.LIMIT_CODE))
        .hasMessageContaining("more than 1000 parts");
  }

  @Test
  void repeatedRowsCannotExpandBeyondTheRowLimit() throws IOException {
    String repeated =
        "<table:table-row table:number-rows-repeated=\"1000\">" + cell("X") + "</table:table-row>";
    byte[] ods = ods(rows(repeated.repeat(101)), 0);

    assertThatThrownBy(() -> reader.read("repeated.ods", ods))
        .isInstanceOfSatisfying(
            BusinessRuleException.class,
            e -> assertThat(e.getCode()).isEqualTo("BULK_FILE_TOO_LARGE"));
  }

  @Test
  void extremeOrMalformedNumbersAndRepeatsStayHarmless() throws IOException {
    byte[] ods =
        ods(
            rows(
                "<table:table-row>"
                    + cell("A")
                    + cell("B")
                    + "</table:table-row>"
                    + "<table:table-row>"
                    + "<table:table-cell office:value-type=\"float\" office:value=\"1E+999999999\"/>"
                    + "<table:table-cell office:value-type=\"float\" office:value=\"not-a-number\""
                    + " table:number-columns-repeated=\"99999999999\"/>"
                    + "</table:table-row>"),
            0);

    ParsedFile file = reader.read("numbers.ods", ods);

    assertThat(file.rows())
        .singleElement()
        .satisfies(
            r ->
                assertThat(r.values())
                    .containsEntry("A", "1E+999999999")
                    .containsEntry("B", "not-a-number"));
  }

  @Test
  void anExcelZipBombIsRefused() throws IOException {
    byte[] bomb = inflatedSheet(workbook(), 20 * MB);
    assertThat(bomb.length).isLessThan(MB);

    assertThatThrownBy(() -> reader.read("bomb.xlsx", bomb))
        .isInstanceOfSatisfying(
            BusinessRuleException.class,
            e -> assertThat(e.getCode()).isEqualTo("BULK_FILE_UNREADABLE"))
        .hasMessageContaining("Zip bomb");
  }

  @Test
  void anOrdinaryExcelFileIsRead() throws IOException {
    ParsedFile file = reader.read("ok.xlsx", workbook());

    assertThat(file.headers()).containsExactly("Code");
    assertThat(file.rows())
        .singleElement()
        .satisfies(r -> assertThat(r.values()).containsEntry("Code", "A1"));
  }

  @Test
  void textUploadsMustBeUtf8TextWithoutBinaryData() {
    byte[] nul = "Code\nA1\u0000\n".getBytes(StandardCharsets.UTF_8);
    byte[] latin1 = "Code\nCaf\u00e9\n".getBytes(StandardCharsets.ISO_8859_1);
    byte[] bom = "\uFEFFCode\nA1\n".getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> reader.read("nul.csv", nul))
        .isInstanceOfSatisfying(
            BusinessRuleException.class, e -> assertThat(e.getCode()).isEqualTo("FILE_NOT_TEXT"));
    assertThatThrownBy(() -> reader.read("latin1.txt", latin1))
        .isInstanceOfSatisfying(
            BusinessRuleException.class,
            e -> assertThat(e.getCode()).isEqualTo("FILE_TEXT_ENCODING"));
    assertThat(reader.read("bom.csv", bom).headers()).containsExactly("Code");
  }

  private static String cell(String text) {
    return "<table:table-cell office:value-type=\"string\"><text:p>"
        + text
        + "</text:p></table:table-cell>";
  }

  private static String rows(String rows) {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><office:document-content xmlns:office=\""
        + OFFICE_NS
        + "\" xmlns:table=\""
        + TABLE_NS
        + "\" xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\"><office:body>"
        + "<office:spreadsheet><table:table table:name=\"Data\">"
        + rows
        + "</table:table></office:spreadsheet></office:body></office:document-content>";
  }

  private static byte[] ods(String contentXml, int extraParts) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      zip.putNextEntry(new ZipEntry("mimetype"));
      zip.write(
          "application/vnd.oasis.opendocument.spreadsheet".getBytes(StandardCharsets.US_ASCII));
      for (int i = 0; i < extraParts; i++) {
        zip.putNextEntry(new ZipEntry("Pictures/p" + i + ".txt"));
      }
      zip.putNextEntry(new ZipEntry("content.xml"));
      zip.write(contentXml.getBytes(StandardCharsets.UTF_8));
    }
    return out.toByteArray();
  }

  private static byte[] workbook() throws IOException {
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      var sheet = wb.createSheet("Data");
      sheet.createRow(0).createCell(0).setCellValue("Code");
      sheet.createRow(1).createCell(0).setCellValue("A1");
      wb.write(out);
      return out.toByteArray();
    }
  }

  /** Copies a workbook and appends blanks after the root element of its sheet (valid XML). */
  private static byte[] inflatedSheet(byte[] xlsx, int blanks) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(xlsx));
        ZipOutputStream zip = new ZipOutputStream(out)) {
      for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
        byte[] part = in.readAllBytes();
        zip.putNextEntry(new ZipEntry(e.getName()));
        zip.write(part);
        if (e.getName().startsWith("xl/worksheets/")) {
          byte[] padding = new byte[blanks];
          java.util.Arrays.fill(padding, (byte) ' ');
          zip.write(padding);
        }
      }
    }
    return out.toByteArray();
  }

  @Test
  void theLimitsAreAppliedToApachePoi() {
    OfficeFileLimits.requireEntryCount(OfficeFileLimits.MAX_ENTRIES);
    assertThat(
            List.of(
                org.apache.poi.openxml4j.util.ZipSecureFile.getMaxEntrySize(),
                org.apache.poi.openxml4j.util.ZipSecureFile.getMaxFileCount()))
        .containsExactly(OfficeFileLimits.MAX_ENTRY_BYTES, (long) OfficeFileLimits.MAX_ENTRIES);
    assertThat(org.apache.poi.openxml4j.util.ZipSecureFile.getMinInflateRatio())
        .isEqualTo(OfficeFileLimits.MIN_INFLATE_RATIO);
  }
}
