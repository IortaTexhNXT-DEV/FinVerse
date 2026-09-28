package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.bulk.service.ParsedFile.RawRow;
import com.iortatechnxt.brokerverse.common.excel.GuidedTables;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

/**
 * Reads the data sheet of an uploaded XLSX, CSV (UTF-8, comma separated, RFC 4180 quotes) or ODS
 * file (BRNB.064 "accept different file formats"), or a plain-text TXT file laid out as the handler
 * declares (delimited or fixed width, CSHID.008). A guided template is read as it was downloaded:
 * the header row is found after its column guide (or by the expected headers), the star of a
 * mandatory header is ignored and the example rows are skipped; a plain file has its headers in the
 * first row. Dates are returned as yyyy-MM-dd, numbers without grouping; blank rows are skipped.
 * Row numbers are those of the file.
 */
@Component
public class BulkFileReader {

  /**
   * Reads a file.
   *
   * @param fileName file name (decides the format)
   * @param content bytes
   * @return headers and rows
   */
  public ParsedFile read(String fileName, byte[] content) {
    return read(fileName, content, TextLayout.AUTO);
  }

  /**
   * Reads a file; a TXT file follows the given layout.
   *
   * @param fileName file name (decides the format)
   * @param content bytes
   * @param layout layout of a TXT file
   * @return headers and rows
   */
  public ParsedFile read(String fileName, byte[] content, TextLayout layout) {
    return read(fileName, content, layout, List.of());
  }

  /**
   * Reads a file whose headers are known: the header row (and, in a workbook, the sheet) holding
   * the most of them is taken when the file has no column guide.
   *
   * @param fileName file name (decides the format)
   * @param content bytes
   * @param layout layout of a TXT file
   * @param expected expected headers
   * @return headers and rows
   */
  public ParsedFile read(
      String fileName, byte[] content, TextLayout layout, Collection<String> expected) {
    String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
    try {
      return toParsed(table(name, content, layout, expected), expected);
    } catch (IOException | SAXException | ParserConfigurationException e) {
      throw new BusinessRuleException(
          "BULK_FILE_UNREADABLE", "The file cannot be read: " + e.getMessage(), e);
    }
  }

  private static List<List<String>> table(
      String name, byte[] content, TextLayout layout, Collection<String> expected)
      throws IOException, SAXException, ParserConfigurationException {
    if (name.endsWith(".txt")) {
      return TextTableReader.read(content, layout);
    }
    if (name.endsWith(".xlsx")) {
      return XlsxTableReader.read(content, expected);
    }
    if (name.endsWith(".csv")) {
      return CsvParser.parse(new String(content, StandardCharsets.UTF_8));
    }
    if (name.endsWith(".ods")) {
      return OdsTableReader.read(content);
    }
    throw new BusinessRuleException(
        "BULK_FILE_TYPE",
        "Upload an Excel (.xlsx), OpenDocument (.ods), CSV (.csv) or text (.txt) file");
  }

  private static ParsedFile toParsed(List<List<String>> table, Collection<String> expected) {
    if (table.isEmpty()) {
      throw new BusinessRuleException("BULK_FILE_EMPTY", "The file has no header row");
    }
    int headerRow = GuidedTables.headerRow(table, expected);
    List<String> headers = GuidedTables.headers(table.get(headerRow));
    List<RawRow> rows = new ArrayList<>();
    for (int i = headerRow + 1; i < table.size(); i++) {
      if (GuidedTables.isExample(table.get(i))) {
        continue;
      }
      Map<String, String> values = values(headers, table.get(i));
      if (!values.isEmpty()) {
        rows.add(new RawRow(i + 1, values));
      }
    }
    return new ParsedFile(headers, rows);
  }

  private static Map<String, String> values(List<String> headers, List<String> cells) {
    Map<String, String> values = new HashMap<>();
    int columns = Math.min(headers.size(), cells.size());
    for (int c = 0; c < columns; c++) {
      String v = cells.get(c);
      if (v != null && !v.isBlank() && !headers.get(c).isEmpty()) {
        values.put(headers.get(c), v);
      }
    }
    return values;
  }
}
