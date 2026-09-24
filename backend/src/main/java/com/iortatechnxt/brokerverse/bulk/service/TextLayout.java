package com.iortatechnxt.brokerverse.bulk.service;

import java.util.List;

/**
 * How a bulk handler's plain-text (.txt) files are laid out (BRQID.006, CSHID.008: bank and
 * collection files such as Bills Payment FS01, Trade and Direct Credit).
 *
 * <ul>
 *   <li>{@link Kind#DELIMITED}: one record per line, fields separated by {@link #delimiter}; the
 *       first line holds the headers unless {@link #headers} are given. A delimiter of {@code 0}
 *       detects tab, pipe, semicolon or comma from the first line.
 *   <li>{@link Kind#FIXED_WIDTH}: one record per line, each field at a fixed position.
 * </ul>
 *
 * The exact record layouts of BDOI's files are parked (OQ03, OQ04): each handler declares its own
 * layout and can later read it from configuration.
 *
 * @param kind delimited or fixed width
 * @param delimiter field separator of a delimited file; {@code 0} = detect
 * @param headers column headers of a delimited file without a header line; empty = first line
 * @param fields fields of a fixed-width file
 */
public record TextLayout(Kind kind, char delimiter, List<String> headers, List<FixedField> fields) {

  /** Delimited with a header line, delimiter detected from that line (the default). */
  public static final TextLayout AUTO = new TextLayout(Kind.DELIMITED, '\0', List.of(), List.of());

  /** Defensive copies. */
  public TextLayout {
    headers = headers == null ? List.of() : List.copyOf(headers);
    fields = fields == null ? List.of() : List.copyOf(fields);
  }

  /**
   * A delimited layout whose first line holds the headers.
   *
   * @param delimiter field separator
   * @return layout
   */
  public static TextLayout delimited(char delimiter) {
    return new TextLayout(Kind.DELIMITED, delimiter, List.of(), List.of());
  }

  /**
   * A delimited layout without a header line.
   *
   * @param delimiter field separator
   * @param headers column headers in field order
   * @return layout
   */
  public static TextLayout delimited(char delimiter, List<String> headers) {
    return new TextLayout(Kind.DELIMITED, delimiter, headers, List.of());
  }

  /**
   * A fixed-width layout.
   *
   * @param fields fields with their position
   * @return layout
   */
  public static TextLayout fixedWidth(List<FixedField> fields) {
    return new TextLayout(Kind.FIXED_WIDTH, '\0', List.of(), fields);
  }

  /** Layout kinds. */
  public enum Kind {
    /** Separated fields. */
    DELIMITED,
    /** Fields at fixed positions. */
    FIXED_WIDTH
  }

  /**
   * One field of a fixed-width record.
   *
   * @param header column header it maps to
   * @param start first position, 1-based
   * @param length number of characters
   */
  public record FixedField(String header, int start, int length) {}
}
