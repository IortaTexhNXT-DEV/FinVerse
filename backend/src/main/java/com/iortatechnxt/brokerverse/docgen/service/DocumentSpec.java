package com.iortatechnxt.brokerverse.docgen.service;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

/**
 * A generated business document, rendered as PDF or Word ({@link DocumentComposer}).
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

  /**
   * A signature caption with the name of the person who signs, e.g. "Prepared by: Maria Santos";
   * the caption alone while nobody has done the step.
   *
   * @param caption caption
   * @param name display name, may be null
   * @return caption text
   */
  public static String signature(String caption, String name) {
    return name == null || name.isBlank() ? caption : caption + ": " + name;
  }

  /** A content block (typed in JSON, for the Word rendition of a composed PDF). */
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = Fields.class, name = "FIELDS"),
    @JsonSubTypes.Type(value = Table.class, name = "TABLE"),
    @JsonSubTypes.Type(value = Text.class, name = "TEXT")
  })
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
   * @param widths relative column widths, one per header (a long text column wider); empty = equal
   */
  public record Table(
      String heading,
      List<String> headers,
      List<List<String>> rows,
      List<Integer> rightAligned,
      List<Float> widths)
      implements Section {

    /** Defensive copies; widths must match the headers when given. */
    public Table {
      headers = List.copyOf(headers);
      rows = rows.stream().map(List::copyOf).toList();
      rightAligned = rightAligned == null ? List.of() : List.copyOf(rightAligned);
      widths = widths == null ? List.of() : List.copyOf(widths);
      if (!widths.isEmpty() && widths.size() != headers.size()) {
        throw new IllegalArgumentException("One width per column: " + headers.size());
      }
    }

    /**
     * A table with columns of equal width.
     *
     * @param heading heading, may be null
     * @param headers column headers
     * @param rows rows (already formatted text)
     * @param rightAligned indexes of columns aligned right (amounts)
     */
    public Table(
        String heading, List<String> headers, List<List<String>> rows, List<Integer> rightAligned) {
      this(heading, headers, rows, rightAligned, List.of());
    }

    /**
     * The relative widths of the columns (equal when none were given).
     *
     * @return one weight per column
     */
    public float[] columnWeights() {
      float[] w = new float[headers.size()];
      for (int i = 0; i < w.length; i++) {
        w[i] = widths.isEmpty() ? 1f : widths.get(i);
      }
      return w;
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
