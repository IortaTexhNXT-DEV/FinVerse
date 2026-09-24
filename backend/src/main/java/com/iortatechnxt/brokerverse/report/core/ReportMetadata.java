package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.List;

/**
 * Catalogue entry describing a report.
 *
 * <p>Viewing (on-screen run) needs {@link #permission}; downloading or printing (export) needs
 * {@link #exportPermission}, which is the same permission unless the report splits them, as the
 * Operations reports do with {@code OPS_REPORT_VIEW} / {@code OPS_REPORT_EXPORT} (CSHID.017/018).
 * Runs and exports of an {@link #archived} report are kept in the report archive ({@code
 * report_run}), exports with their file.
 *
 * @param code report code (e.g. "GL-TB", "PGIBR015")
 * @param title title
 * @param category menu group
 * @param description one line purpose
 * @param parameters parameters
 * @param permission permission required to run (view)
 * @param exportPermission permission required to export (download, print)
 * @param archived whether runs and exports are archived
 */
public record ReportMetadata(
    String code,
    String title,
    ReportCategory category,
    String description,
    List<ParameterSpec> parameters,
    Permission permission,
    Permission exportPermission,
    boolean archived) {

  /** Canonical constructor copying the parameter list; export defaults to the view permission. */
  public ReportMetadata {
    parameters = List.copyOf(parameters);
    exportPermission = exportPermission == null ? permission : exportPermission;
  }

  /**
   * A report viewed and exported with one permission, not archived.
   *
   * @param code report code
   * @param title title
   * @param category menu group
   * @param description one line purpose
   * @param parameters parameters
   * @param permission permission required to run and export
   */
  public ReportMetadata(
      String code,
      String title,
      ReportCategory category,
      String description,
      List<ParameterSpec> parameters,
      Permission permission) {
    this(code, title, category, description, parameters, permission, permission, false);
  }

  /**
   * An Operations report (CSHID.017/018): viewed with {@code OPS_REPORT_VIEW}, exported with {@code
   * OPS_REPORT_EXPORT}, archived.
   *
   * @param code report code (e.g. {@code CSH-APPLIED-PREM})
   * @param title title
   * @param description one line purpose
   * @param parameters parameters
   * @return metadata in the Operations category
   */
  public static ReportMetadata operations(
      String code, String title, String description, List<ParameterSpec> parameters) {
    return new ReportMetadata(
        code,
        title,
        ReportCategory.OPERATIONS,
        description,
        parameters,
        Permission.OPS_REPORT_VIEW,
        Permission.OPS_REPORT_EXPORT,
        true);
  }
}
