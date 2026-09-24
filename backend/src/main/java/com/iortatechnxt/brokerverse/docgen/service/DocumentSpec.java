package com.iortatechnxt.brokerverse.docgen.service;

import java.util.List;

/**
 * A generated PDF document.
 *
 * @param companyName issuing company (letterhead)
 * @param title document title
 * @param reference document number / reference
 * @param sections content blocks in order
 * @param signatures signature captions (e.g. "Prepared by", "Approved by")
 * @param footer small print at the foot of every page (e.g. template version)
 */
public record DocumentSpec(
    String companyName,
    String title,
    String reference,
    List<Section> sections,
    List<String> signatures,
    String footer) {

  /** Defensive copies. */
  public DocumentSpec {
    sections = List.copyOf(sections);
    signatures = signatures == null ? List.of() : List.copyOf(signatures);
  }

  /** A content block. */
  public sealed interface Section permits Fields, Table, Text {}

  /**
   * Label / value pairs in two columns.
   *
   * @param heading heading, may be null
   * @param fields pairs
   */
  public record Fields(String heading, List<Field> fields) implements Section {

    /** Defensive copy. */
    public Fields {
      fields = List.copyOf(fields);
    }
  }

  /**
   * One label / value.
   *
   * @param label label
   * @param value value
   */
  public record Field(String label, String value) {}

  /**
   * A table.
   *
   * @param heading heading, may be null
   * @param headers column headers
   * @param rows rows (already formatted text)
   * @param rightAligned indexes of columns aligned right (amounts)
   */
  public record Table(
      String heading, List<String> headers, List<List<String>> rows, List<Integer> rightAligned)
      implements Section {

    /** Defensive copies. */
    public Table {
      headers = List.copyOf(headers);
      rows = rows.stream().map(List::copyOf).toList();
      rightAligned = rightAligned == null ? List.of() : List.copyOf(rightAligned);
    }
  }

  /**
   * Free text (merged template).
   *
   * @param heading heading, may be null
   * @param body text; blank lines separate paragraphs
   */
  public record Text(String heading, String body) implements Section {}
}
