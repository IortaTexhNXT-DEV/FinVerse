package com.iortatechnxt.finverse.report.core;

import com.iortatechnxt.finverse.security.domain.Permission;
import java.util.List;

/**
 * Catalogue entry describing a report.
 *
 * @param code report code (e.g. "GL-TB", "PGIBR015")
 * @param title title
 * @param category menu group
 * @param description one line purpose
 * @param parameters parameters
 * @param permission permission required to run
 */
public record ReportMetadata(
    String code,
    String title,
    ReportCategory category,
    String description,
    List<ParameterSpec> parameters,
    Permission permission) {

  /** Canonical constructor copying the parameter list. */
  public ReportMetadata {
    parameters = List.copyOf(parameters);
  }
}
