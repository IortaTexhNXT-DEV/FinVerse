package com.iortatechnxt.finverse.report.core;

/**
 * A report. Implementations are Spring beans discovered automatically by {@link ReportRegistry}.
 *
 * <p>To add a report: implement this interface in the owning module, declare metadata and build a
 * {@link ReportResult} (typically with {@link TabularReportBuilder}). No other wiring needed.
 */
public interface ReportDefinition {

  /**
   * Describes the report.
   *
   * @return metadata
   */
  ReportMetadata metadata();

  /**
   * Produces the report data.
   *
   * @param params validated parameters
   * @return result
   */
  ReportResult generate(ReportParameters params);
}
