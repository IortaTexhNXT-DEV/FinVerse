package com.iortatechnxt.finverse.report.render;

import java.time.Instant;

/**
 * Print header context (standard report header of the GI report book).
 *
 * @param companyName company printed at the top
 * @param generatedBy user id
 * @param generatedAt run time
 * @param footerText text printed at the foot of every page ({@code REPORT_FOOTER_TEXT}), blank for
 *     none
 */
public record ReportContext(
    String companyName, String generatedBy, Instant generatedAt, String footerText) {

  /** Normalises a missing footer to an empty text. */
  public ReportContext {
    footerText = footerText == null ? "" : footerText.strip();
  }
}
