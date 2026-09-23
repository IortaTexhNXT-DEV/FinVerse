package com.iortatechnxt.finverse.report.render;

import com.iortatechnxt.finverse.report.core.ReportResult;

/** Renders a report result into a file format. */
public interface ReportRenderer {

  /**
   * Format produced.
   *
   * @return format
   */
  ExportFormat format();

  /**
   * Renders the report.
   *
   * @param result report data
   * @param context print context
   * @return file bytes
   */
  byte[] render(ReportResult result, ReportContext context);
}
