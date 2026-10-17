package com.iortatechnxt.brokerverse.docgen.api.dto;

/**
 * Whether a downloaded PDF can also be downloaded as Word (client requirement 16).
 *
 * @param available true when the PDF was composed here and its content is recorded
 * @param title document title, null when not available
 */
public record WordAvailability(boolean available, String title) {

  /**
   * Maps the title of a document with a Word copy.
   *
   * @param title document title, null when the PDF has no Word copy
   * @return availability
   */
  public static WordAvailability from(String title) {
    return new WordAvailability(title != null, title);
  }
}
