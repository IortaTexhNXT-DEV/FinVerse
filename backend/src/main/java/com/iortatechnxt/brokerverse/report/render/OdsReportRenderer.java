package com.iortatechnxt.brokerverse.report.render;

import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.springframework.stereotype.Component;

/**
 * OpenDocument spreadsheet export (.ods, BRNB.037): the same layout as the Excel export (company,
 * title, report ID / user / run date, the filters, the column headings and the rows), with numeric
 * and date cells typed so that LibreOffice and Excel can calculate with them.
 */
@Component
public class OdsReportRenderer implements ReportRenderer {

  private static final String MIME = "application/vnd.oasis.opendocument.spreadsheet";
  private static final String OFFICE = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";
  private static final String TABLE = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
  private static final String TEXT = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
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
  private static final String VALUE_TYPE = "value-type";
  private static final String P_OFFICE = "office";
  private static final String CELL = "table-cell";
  private static final String ROW = "table-row";
  private static final DateTimeFormatter STAMP =
      DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZoneId.of("Asia/Manila"));

  @Override
  public ExportFormat format() {
    return ExportFormat.ODS;
  }

  @Override
  public byte[] render(ReportResult result, ReportContext context) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      stored(zip, MIME.getBytes(StandardCharsets.US_ASCII));
      deflated(zip, "META-INF/manifest.xml", MANIFEST.getBytes(StandardCharsets.UTF_8));
      deflated(zip, "content.xml", content(result, context));
    } catch (IOException e) {
      throw new UncheckedIOException("ODS rendering failed", e);
    }
    return out.toByteArray();
  }

  private static void stored(ZipOutputStream zip, byte[] bytes) throws IOException {
    ZipEntry entry = new ZipEntry("mimetype");
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

  private static byte[] content(ReportResult result, ReportContext ctx) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      XMLStreamWriter xml =
          XMLOutputFactory.newFactory().createXMLStreamWriter(out, StandardCharsets.UTF_8.name());
      xml.writeStartDocument("UTF-8", "1.0");
      xml.writeStartElement(P_OFFICE, "document-content", OFFICE);
      xml.writeNamespace(P_OFFICE, OFFICE);
      xml.writeNamespace("table", TABLE);
      xml.writeNamespace("text", TEXT);
      xml.writeAttribute(P_OFFICE, OFFICE, "version", "1.2");
      xml.writeStartElement(OFFICE, "body");
      xml.writeStartElement(OFFICE, "spreadsheet");
      xml.writeStartElement(TABLE, "table");
      xml.writeAttribute("table", TABLE, "name", result.code());
      writeHeader(xml, result, ctx);
      writeRows(xml, result);
      xml.writeEndElement();
      xml.writeEndElement();
      xml.writeEndElement();
      xml.writeEndElement();
      xml.writeEndDocument();
      xml.close();
    } catch (XMLStreamException e) {
      throw new IllegalStateException("ODS rendering failed", e);
    }
    return out.toByteArray();
  }

  private static void writeHeader(XMLStreamWriter xml, ReportResult result, ReportContext ctx)
      throws XMLStreamException {
    textRow(xml, List.of(ctx.companyName()));
    textRow(xml, List.of(result.title()));
    textRow(
        xml,
        List.of(
            "Report ID: "
                + result.code()
                + "   User ID: "
                + ctx.generatedBy()
                + "   Run Date: "
                + STAMP.format(ctx.generatedAt())));
    for (String line : result.parameterEcho()) {
      textRow(xml, List.of(line));
    }
    textRow(xml, List.of());
    List<String> headings = new ArrayList<>();
    headings.add("");
    result.columns().forEach(c -> headings.add(c.label()));
    textRow(xml, headings);
  }

  private static void writeRows(XMLStreamWriter xml, ReportResult result)
      throws XMLStreamException {
    for (ReportRow row : result.rows()) {
      xml.writeStartElement(TABLE, ROW);
      String label = row.label() == null ? "" : "  ".repeat(row.level()) + row.label();
      textCell(xml, row.kind() == RowKind.DETAIL ? "" : label);
      for (ReportColumn c : result.columns()) {
        cell(xml, row.cells().get(c.key()), c.type());
      }
      xml.writeEndElement();
    }
    for (String note : result.notes()) {
      textRow(xml, List.of("Note: " + note));
    }
  }

  private static void textRow(XMLStreamWriter xml, List<String> cells) throws XMLStreamException {
    xml.writeStartElement(TABLE, ROW);
    if (cells.isEmpty()) {
      xml.writeEmptyElement(TABLE, CELL);
    }
    for (String text : cells) {
      textCell(xml, text);
    }
    xml.writeEndElement();
  }

  private static void cell(XMLStreamWriter xml, Object value, ColumnType type)
      throws XMLStreamException {
    if (value instanceof Number && type != ColumnType.TEXT) {
      String number = CellFormatter.toDecimal(value).toPlainString();
      xml.writeStartElement(TABLE, CELL);
      xml.writeAttribute(P_OFFICE, OFFICE, VALUE_TYPE, "float");
      xml.writeAttribute(P_OFFICE, OFFICE, "value", number);
      paragraph(xml, CellFormatter.format(value, type));
      xml.writeEndElement();
    } else if (value instanceof LocalDate d) {
      xml.writeStartElement(TABLE, CELL);
      xml.writeAttribute(P_OFFICE, OFFICE, VALUE_TYPE, "date");
      xml.writeAttribute(P_OFFICE, OFFICE, "date-value", d.toString());
      paragraph(xml, CellFormatter.format(d, ColumnType.DATE));
      xml.writeEndElement();
    } else {
      textCell(xml, value == null ? "" : CellFormatter.format(value, type));
    }
  }

  private static void textCell(XMLStreamWriter xml, String text) throws XMLStreamException {
    xml.writeStartElement(TABLE, CELL);
    xml.writeAttribute(P_OFFICE, OFFICE, VALUE_TYPE, "string");
    paragraph(xml, text);
    xml.writeEndElement();
  }

  private static void paragraph(XMLStreamWriter xml, String text) throws XMLStreamException {
    xml.writeStartElement(TEXT, "p");
    xml.writeCharacters(text);
    xml.writeEndElement();
  }
}
