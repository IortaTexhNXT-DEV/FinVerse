package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes a one-sheet OpenDocument spreadsheet (.ods), the alternative CLPC billing file format
 * (BRNB.067 "save the file as .xlsx or .ods"). Numbers are numeric cells, dates date cells and
 * booleans Y / N.
 */
public final class OdsSpreadsheetWriter {

  private static final String MIME = "application/vnd.oasis.opendocument.spreadsheet";
  private static final String MANIFEST =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" \
      manifest:version="1.2">
       <manifest:file-entry manifest:full-path="/" manifest:version="1.2" \
      manifest:media-type="application/vnd.oasis.opendocument.spreadsheet"/>
       <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
      </manifest:manifest>
      """;
  private static final String CONTENT_HEAD =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
          + "<office:document-content"
          + " xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\""
          + " xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\""
          + " xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\""
          + " office:version=\"1.2\"><office:body><office:spreadsheet>";
  private static final String CONTENT_TAIL =
      "</office:spreadsheet></office:body></office:document-content>";
  private static final String CELL_END = "</text:p></table:table-cell>";
  private static final int CONTENT_BUFFER = 8192;

  private OdsSpreadsheetWriter() {}

  /**
   * Renders the sheet.
   *
   * @param spec sheet name, headers and rows
   * @return .ods bytes
   */
  public static byte[] write(SheetSpec spec) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      stored(zip, "mimetype", MIME.getBytes(StandardCharsets.US_ASCII));
      deflated(zip, "META-INF/manifest.xml", MANIFEST.getBytes(StandardCharsets.UTF_8));
      deflated(zip, "content.xml", content(spec).getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException("Could not write the spreadsheet", e);
    }
    return out.toByteArray();
  }

  private static void stored(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
    ZipEntry entry = new ZipEntry(name);
    entry.setMethod(ZipEntry.STORED);
    entry.setSize(bytes.length);
    CRC32 crc = new CRC32();
    crc.update(bytes);
    entry.setCrc(crc.getValue());
    zip.putNextEntry(entry);
    zip.write(bytes);
    zip.closeEntry();
  }

  private static void deflated(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(bytes);
    zip.closeEntry();
  }

  private static String content(SheetSpec spec) {
    StringBuilder xml = new StringBuilder(CONTENT_BUFFER);
    xml.append(CONTENT_HEAD)
        .append("<table:table table:name=\"")
        .append(escape(spec.sheetName()))
        .append("\"><table:table-row>");
    spec.headers().forEach(h -> xml.append(textCell(h)));
    xml.append("</table:table-row>");
    for (List<Object> row : spec.rows()) {
      xml.append("<table:table-row>");
      row.forEach(v -> xml.append(cell(v)));
      xml.append("</table:table-row>");
    }
    xml.append("</table:table>").append(CONTENT_TAIL);
    return xml.toString();
  }

  private static String cell(Object value) {
    return switch (value) {
      case null -> "<table:table-cell/>";
      case BigDecimal n -> numberCell(n.toPlainString());
      case Number n -> numberCell(n.toString());
      case LocalDate d ->
          "<table:table-cell office:value-type=\"date\" office:date-value=\""
              + d
              + "\"><text:p>"
              + d
              + CELL_END;
      case Boolean b -> textCell(Boolean.TRUE.equals(b) ? "Y" : "N");
      default -> textCell(value.toString());
    };
  }

  private static String numberCell(String number) {
    return "<table:table-cell office:value-type=\"float\" office:value=\""
        + number
        + "\"><text:p>"
        + number
        + CELL_END;
  }

  private static String textCell(String text) {
    return "<table:table-cell office:value-type=\"string\"><text:p>" + escape(text) + CELL_END;
  }

  static String escape(String text) {
    StringBuilder sb = new StringBuilder(text.length());
    for (char c : text.toCharArray()) {
      switch (c) {
        case '&' -> sb.append("&amp;");
        case '<' -> sb.append("&lt;");
        case '>' -> sb.append("&gt;");
        case '"' -> sb.append("&quot;");
        default -> sb.append(c);
      }
    }
    return sb.toString();
  }
}
