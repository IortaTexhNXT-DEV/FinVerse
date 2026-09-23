package com.iortatechnxt.finverse.report.api.dto;

import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
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
 */
public record ReportCatalogueEntry(
    String code,
    String title,
    String category,
    String categoryLabel,
    String description,
    List<ParameterSpec> parameters) {

  /**
   * Maps metadata.
   *
   * @param m metadata
   * @return entry
   */
  public static ReportCatalogueEntry from(ReportMetadata m) {
    return new ReportCatalogueEntry(
        m.code(),
        m.title(),
        m.category().name(),
        m.category().label(),
        m.description(),
        m.parameters());
  }
}
