package com.iortatechnxt.brokerverse.report.render;

import java.time.Instant;

/**
 * Print header context (standard report header of the GI report book).
 *
 * @param companyName company printed at the top
 * @param generatedBy user id
 * @param generatedAt run time
 * @param footerText text printed at the foot of every page ({@code REPORT_FOOTER_TEXT}), blank for
 *     none
 * @param print PDF print options (FRBS 2.4.9)
 */
public record ReportContext(
    String companyName,
    String generatedBy,
    Instant generatedAt,
    String footerText,
    PrintOptions print) {

  /** Normalises a missing footer to an empty text and missing options to the default. */
  public ReportContext {
    footerText = footerText == null ? "" : footerText.strip();
    print = print == null ? PrintOptions.DEFAULT : print;
  }

  /**
   * Context with the default print options.
   *
   * @param companyName company printed at the top
   * @param generatedBy user id
   * @param generatedAt run time
   * @param footerText footer text
   */
  public ReportContext(
      String companyName, String generatedBy, Instant generatedAt, String footerText) {
    this(companyName, generatedBy, generatedAt, footerText, PrintOptions.DEFAULT);
  }

  /**
   * The same context with other print options.
   *
   * @param options print options
   * @return context
   */
  public ReportContext withPrint(PrintOptions options) {
    return new ReportContext(companyName, generatedBy, generatedAt, footerText, options);
  }
}
