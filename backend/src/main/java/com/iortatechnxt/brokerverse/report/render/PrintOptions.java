package com.iortatechnxt.brokerverse.report.render;

import java.util.Locale;

/**
 * PDF print options chosen by the user (FRBS 2.4.9): paper size, orientation and whether the table
 * is stretched to the page width. Headers, column headings and the page footer are repeated on
 * every page whatever the options.
 *
 * @param paper paper size
 * @param orientation orientation; AUTO turns the page to landscape for wide reports
 * @param fitToWidth true stretches the table to the page width, false keeps natural column widths
 */
public record PrintOptions(Paper paper, Orientation orientation, boolean fitToWidth) {

  /** The default layout: A4, orientation by width, fitted to the page. */
  public static final PrintOptions DEFAULT = new PrintOptions(Paper.A4, Orientation.AUTO, true);

  /** Above this many columns AUTO turns the page to landscape. */
  public static final int LANDSCAPE_THRESHOLD = 7;

  /** Nulls become the defaults. */
  public PrintOptions {
    paper = paper == null ? Paper.A4 : paper;
    orientation = orientation == null ? Orientation.AUTO : orientation;
  }

  /**
   * Options from request values (case-insensitive; blanks and unknown values give the default).
   *
   * @param paper paper name
   * @param orientation orientation name
   * @param fitToWidth fit flag, null = true
   * @return options
   */
  public static PrintOptions of(String paper, String orientation, Boolean fitToWidth) {
    return new PrintOptions(
        parse(Paper.class, paper),
        parse(Orientation.class, orientation),
        fitToWidth == null || fitToWidth);
  }

  /**
   * Text for the parameter echo, empty for the default layout.
   *
   * @return e.g. "Print: LETTER LANDSCAPE, natural width"
   */
  public String echo() {
    if (equals(DEFAULT)) {
      return "";
    }
    return "Print: "
        + paper
        + " "
        + orientation
        + (fitToWidth ? ", fit to width" : ", natural width");
  }

  /**
   * Whether the page is turned for a report with this many columns (AUTO: wide reports).
   *
   * @param columns number of report columns
   * @return true for landscape
   */
  public boolean landscape(int columns) {
    return switch (orientation) {
      case AUTO -> columns > LANDSCAPE_THRESHOLD;
      case PORTRAIT -> false;
      case LANDSCAPE -> true;
    };
  }

  private static <E extends Enum<E>> E parse(Class<E> type, String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  /** Paper sizes. */
  public enum Paper {
    /** ISO A4. */
    A4,
    /** US Letter. */
    LETTER,
    /** US Legal. */
    LEGAL,
    /** ISO A3. */
    A3
  }

  /** Page orientation. */
  public enum Orientation {
    /** Landscape when the report has many columns. */
    AUTO,
    /** Portrait. */
    PORTRAIT,
    /** Landscape. */
    LANDSCAPE
  }
}
