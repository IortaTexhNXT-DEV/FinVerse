package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.office.OfficeFileLimits;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Reads the first table of an OpenDocument spreadsheet (content.xml) into text cells, expanding
 * repeated rows and columns (bounded) and trimming trailing blank cells. The XML parser is hardened
 * against external entities; the archive is read within {@link OfficeFileLimits} (number of parts,
 * uncompressed size of content.xml, expansion ratio), and the expanded table is limited to 100,000
 * rows of 1,024 columns.
 */
final class OdsTableReader {

  private static final String TABLE_NS = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
  private static final String OFFICE_NS = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";
  private static final int MAX_REPEAT = 1000;
  private static final int MAX_ROWS = 100_000;
  private static final int MAX_COLUMNS = 1024;
  private static final int MAX_NUMBER_DIGITS = 100;
  private static final int ISO_DATE_LENGTH = 10;

  private OdsTableReader() {}

  /**
   * Reads a spreadsheet.
   *
   * @param content ods bytes
   * @return rows of cells
   * @throws IOException when the archive cannot be read
   * @throws SAXException when content.xml is malformed
   * @throws ParserConfigurationException when the XML parser cannot be configured
   */
  static List<List<String>> read(byte[] content)
      throws IOException, SAXException, ParserConfigurationException {
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
      int entries = 0;
      ZipEntry entry = zip.getNextEntry();
      while (entry != null) {
        OfficeFileLimits.requireEntryCount(++entries);
        if ("content.xml".equals(entry.getName())) {
          return table(parse(OfficeFileLimits.boundedEntry(zip, content.length)));
        }
        entry = zip.getNextEntry();
      }
    }
    throw new BusinessRuleException(
        "BULK_FILE_UNREADABLE", "The file is not an OpenDocument spreadsheet");
  }

  private static Document parse(InputStream in)
      throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
    f.setNamespaceAware(true);
    f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    f.setXIncludeAware(false);
    f.setExpandEntityReferences(false);
    return f.newDocumentBuilder().parse(in);
  }

  private static List<List<String>> table(Document doc) {
    NodeList tables = doc.getElementsByTagNameNS(TABLE_NS, "table");
    List<List<String>> result = new ArrayList<>();
    if (tables.getLength() == 0) {
      return result;
    }
    NodeList rows = ((Element) tables.item(0)).getElementsByTagNameNS(TABLE_NS, "table-row");
    for (int r = 0; r < rows.getLength(); r++) {
      Element row = (Element) rows.item(r);
      List<String> cells = cells(row);
      int times = cells.isEmpty() ? 1 : repeat(row, "number-rows-repeated");
      if (result.size() + times > MAX_ROWS) {
        throw new BusinessRuleException(
            "BULK_FILE_TOO_LARGE", "The spreadsheet has more than " + MAX_ROWS + " rows");
      }
      for (int k = 0; k < times; k++) {
        result.add(cells);
      }
    }
    return result;
  }

  private static List<String> cells(Element row) {
    List<String> cells = new ArrayList<>();
    NodeList children = row.getChildNodes();
    for (int c = 0; c < children.getLength(); c++) {
      if (children.item(c) instanceof Element cell && isCell(cell)) {
        String value = value(cell);
        int times = repeat(cell, "number-columns-repeated");
        for (int k = 0; k < times && cells.size() < MAX_COLUMNS; k++) {
          cells.add(value);
        }
      }
    }
    while (!cells.isEmpty() && cells.get(cells.size() - 1).isBlank()) {
      cells.remove(cells.size() - 1);
    }
    return cells;
  }

  private static boolean isCell(Element e) {
    return TABLE_NS.equals(e.getNamespaceURI())
        && ("table-cell".equals(e.getLocalName()) || "covered-table-cell".equals(e.getLocalName()));
  }

  private static String value(Element cell) {
    String type = cell.getAttributeNS(OFFICE_NS, "value-type");
    if ("date".equals(type)) {
      String date = cell.getAttributeNS(OFFICE_NS, "date-value");
      return date.length() >= ISO_DATE_LENGTH ? date.substring(0, ISO_DATE_LENGTH) : date;
    }
    if ("float".equals(type) || "currency".equals(type) || "percentage".equals(type)) {
      return number(cell.getAttributeNS(OFFICE_NS, "value"));
    }
    return cell.getTextContent();
  }

  /** The plain digits of a number; an extreme exponent (e.g. 1E+999999999) stays as written. */
  private static String number(String value) {
    BigDecimal number;
    try {
      number = new BigDecimal(value.trim()).stripTrailingZeros();
    } catch (NumberFormatException e) {
      return value;
    }
    if (Math.abs((long) number.scale()) > MAX_NUMBER_DIGITS
        || number.precision() > MAX_NUMBER_DIGITS) {
      return value;
    }
    return number.toPlainString();
  }

  private static int repeat(Element e, String attribute) {
    String v = e.getAttributeNS(TABLE_NS, attribute);
    if (v.isEmpty()) {
      return 1;
    }
    try {
      return Math.max(1, Math.min(Integer.parseInt(v.trim()), MAX_REPEAT));
    } catch (NumberFormatException invalid) {
      return MAX_REPEAT;
    }
  }
}
