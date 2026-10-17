package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.common.excel.WorkbookTables;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/** Reads the data sheet of an Excel workbook into text cells (see {@link WorkbookTables}). */
final class XlsxTableReader {

  private XlsxTableReader() {}

  /**
   * Reads a workbook.
   *
   * @param content xlsx bytes
   * @param expected expected headers (empty when unknown)
   * @return rows of cells
   * @throws IOException when the file is not a readable workbook
   */
  static List<List<String>> read(byte[] content, Collection<String> expected) throws IOException {
    return WorkbookTables.read(content, expected);
  }
}
