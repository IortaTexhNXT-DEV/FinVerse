package com.iortatechnxt.brokerverse.report.core;

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

  /**
   * The name of an exported file without its extension, when the report has a naming convention
   * (for example {@code <Report Name>_<MMDDYYYY>}); the report code otherwise.
   *
   * @param extractionDate date of the extraction
   * @return file name without extension, null for the report code
   */
  default String exportName(java.time.LocalDate extractionDate) {
    return null;
  }
}
