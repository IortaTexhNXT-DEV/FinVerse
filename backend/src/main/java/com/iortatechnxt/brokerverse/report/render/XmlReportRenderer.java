package com.iortatechnxt.brokerverse.report.render;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.springframework.stereotype.Component;

/**
 * XML export (BRNB.037): the print metadata (company, report, user, run time, filters), the columns
 * and every row with typed values. Numbers are written in plain notation and dates in ISO format,
 * so the file can be loaded by other systems without parsing display formats.
 */
@Component
public class XmlReportRenderer implements ReportRenderer {

  private static final String ENCODING = "UTF-8";
  private static final String CODE = "code";
  private static final String LABEL = "label";

  @Override
  public ExportFormat format() {
    return ExportFormat.XML;
  }

  @Override
  public byte[] render(ReportResult result, ReportContext context) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      XMLStreamWriter xml =
          XMLOutputFactory.newFactory().createXMLStreamWriter(out, StandardCharsets.UTF_8.name());
      xml.writeStartDocument(ENCODING, "1.0");
      xml.writeStartElement("report");
      xml.writeAttribute(CODE, result.code());
      xml.writeAttribute("title", result.title());
      writeMetadata(xml, result, context);
      writeColumns(xml, result);
      writeRows(xml, result);
      xml.writeStartElement("notes");
      for (String note : result.notes()) {
        element(xml, "note", note);
      }
      xml.writeEndElement();
      xml.writeEndElement();
      xml.writeEndDocument();
      xml.close();
    } catch (XMLStreamException e) {
      throw new IllegalStateException("XML rendering failed", e);
    }
    return out.toByteArray();
  }

  private static void writeMetadata(XMLStreamWriter xml, ReportResult result, ReportContext ctx)
      throws XMLStreamException {
    xml.writeStartElement("metadata");
    element(xml, "company", ctx.companyName());
    element(xml, "generatedBy", ctx.generatedBy());
    element(xml, "generatedAt", ctx.generatedAt().toString());
    xml.writeStartElement("filters");
    for (String line : result.parameterEcho()) {
      element(xml, "filter", line);
    }
    xml.writeEndElement();
    xml.writeEndElement();
  }

  private static void writeColumns(XMLStreamWriter xml, ReportResult result)
      throws XMLStreamException {
    xml.writeStartElement("columns");
    for (ReportColumn c : result.columns()) {
      xml.writeEmptyElement("column");
      xml.writeAttribute("key", c.key());
      xml.writeAttribute(LABEL, c.label());
      xml.writeAttribute("type", c.type().name());
    }
    xml.writeEndElement();
  }

  private static void writeRows(XMLStreamWriter xml, ReportResult result)
      throws XMLStreamException {
    xml.writeStartElement("rows");
    for (ReportRow row : result.rows()) {
      xml.writeStartElement("row");
      xml.writeAttribute("kind", row.kind().name());
      xml.writeAttribute("level", String.valueOf(row.level()));
      if (row.label() != null) {
        xml.writeAttribute(LABEL, row.label());
      }
      for (ReportColumn c : result.columns()) {
        Object value = row.cells().get(c.key());
        if (value != null) {
          xml.writeStartElement("cell");
          xml.writeAttribute("key", c.key());
          xml.writeCharacters(plain(value));
          xml.writeEndElement();
        }
      }
      xml.writeEndElement();
    }
    xml.writeEndElement();
  }

  private static String plain(Object value) {
    return switch (value) {
      case BigDecimal d -> d.toPlainString();
      case LocalDate d -> d.toString();
      default -> value.toString();
    };
  }

  private static void element(XMLStreamWriter xml, String name, String text)
      throws XMLStreamException {
    xml.writeStartElement(name);
    xml.writeCharacters(text == null ? "" : text);
    xml.writeEndElement();
  }
}
