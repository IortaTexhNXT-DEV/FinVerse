package com.iortatechnxt.brokerverse.report.api.dto;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import java.util.List;

/**
 * Report catalogue entry.
 *
 * @param code code
 * @param title title
 * @param category category key
 * @param categoryLabel category label
 * @param description description
 * @param parameters parameters
 * @param exportable whether the user may download or print it (CSHID.018)
 * @param archived whether runs and exports are archived
 * @param documentStyle whether the report is a document or schedule (Word export offered)
 * @param formats export formats offered, in menu order (client requirement 16)
 */
public record ReportCatalogueEntry(
    String code,
    String title,
    String category,
    String categoryLabel,
    String description,
    List<ParameterSpec> parameters,
    boolean exportable,
    boolean archived,
    boolean documentStyle,
    List<ExportFormat> formats) {

  /**
   * Maps metadata.
   *
   * @param m metadata
   * @param exportable whether the current user may export it
   * @return entry
   */
  public static ReportCatalogueEntry from(ReportMetadata m, boolean exportable) {
    return new ReportCatalogueEntry(
        m.code(),
        m.title(),
        m.category().name(),
        m.category().label(),
        m.description(),
        m.parameters(),
        exportable,
        m.archived(),
        m.documentStyle(),
        m.formats());
  }
}
