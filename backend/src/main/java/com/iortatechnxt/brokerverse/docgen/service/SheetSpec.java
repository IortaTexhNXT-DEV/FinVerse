package com.iortatechnxt.brokerverse.docgen.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A generated spreadsheet (placement file, billing file...).
 *
 * @param sheetName sheet name
 * @param headers column headers
 * @param rows rows; numbers stay numeric, dates are written as dates
 */
public record SheetSpec(String sheetName, List<String> headers, List<List<Object>> rows) {

  /** Defensive copies (values may be null). */
  public SheetSpec {
    headers = List.copyOf(headers);
    rows = rows.stream().map(r -> Collections.unmodifiableList(new ArrayList<>(r))).toList();
  }
}
