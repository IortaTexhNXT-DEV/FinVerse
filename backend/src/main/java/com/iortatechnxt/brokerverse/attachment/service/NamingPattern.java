package com.iortatechnxt.brokerverse.attachment.service;

/**
 * Named patterns of nominated document names (BRNB.026, Q23; SNSRP-601).
 *
 * <ul>
 *   <li>{@link #DEFAULT}: {@code <REFERENCE>_<DOCTYPE>_<n>.<ext>}, the build default of every
 *       module;
 *   <li>{@link #SCREENING}: {@code <FORM_TYPE>_<CLIENT_NAME>_<DATE_RECEIVED
 *       yyyyMMdd>_<DOCTYPE>_<n>.<ext>}, the syntax of the screening documents (BRD-10 p.19).
 * </ul>
 */
public enum NamingPattern {
  DEFAULT("<REFERENCE>_<DOCTYPE>_<n>.<ext>"),
  SCREENING("<FORM_TYPE>_<CLIENT_NAME>_<DATE_RECEIVED yyyyMMdd>_<DOCTYPE>_<n>.<ext>");

  private final String syntax;

  NamingPattern(String syntax) {
    this.syntax = syntax;
  }

  /**
   * The syntax shown to users.
   *
   * @return syntax
   */
  public String syntax() {
    return syntax;
  }
}
