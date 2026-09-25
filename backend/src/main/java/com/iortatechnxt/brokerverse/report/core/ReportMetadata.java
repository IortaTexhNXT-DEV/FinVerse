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

  /**
   * A Collections report (COLLECTIONS_DESIGN section 11): viewed with {@code CLX_REPORT_VIEW},
   * exported with {@code CLX_EXPORT} (caveat p.93), archived.
   *
   * @param code report code (e.g. {@code CLX-OUTSTANDING-PR})
   * @param title title
   * @param description one line purpose
   * @param parameters parameters
   * @return metadata in the Collections category
   */
  public static ReportMetadata collections(
      String code, String title, String description, List<ParameterSpec> parameters) {
    return new ReportMetadata(
        code,
        title,
        ReportCategory.COLLECTIONS,
        description,
        parameters,
        Permission.CLX_REPORT_VIEW,
        Permission.CLX_EXPORT,
        true);
  }

  /**
   * A Disbursement report (ACCOUNTING_DISBURSEMENT_DESIGN section 10, Appendix B): viewed with
   * {@code DISB_REPORT_VIEW}, exported with {@code DISB_REPORT_EXPORT}, archived.
   *
   * @param code report code (e.g. {@code DSB-MASTERLIST})
   * @param title title
   * @param description one line purpose
   * @param parameters parameters
   * @return metadata in the Disbursement category
   */
  public static ReportMetadata disbursement(
      String code, String title, String description, List<ParameterSpec> parameters) {
    return new ReportMetadata(
        code,
        title,
        ReportCategory.DISBURSEMENT,
        description,
        parameters,
        Permission.DISB_REPORT_VIEW,
        Permission.DISB_REPORT_EXPORT,
        true);
  }

  /**
   * An ACSL report (ACCOUNTING_DISBURSEMENT_DESIGN section 10, Appendix C): viewed with {@code
   * ACSL_REPORT_VIEW}, exported with {@code ACSL_REPORT_EXPORT}, archived.
   *
   * @param code report code (e.g. {@code ACSL-SOA-RECON})
   * @param title title
   * @param description one line purpose
   * @param parameters parameters
   * @return metadata in the ACSL category
   */
  public static ReportMetadata acsl(
      String code, String title, String description, List<ParameterSpec> parameters) {
    return new ReportMetadata(
        code,
        title,
        ReportCategory.ACSL,
        description,
        parameters,
        Permission.ACSL_REPORT_VIEW,
        Permission.ACSL_REPORT_EXPORT,
        true);
  }

  /**
   * A report of the BDOI report pack (ACCOUNTING_DISBURSEMENT_DESIGN section 10, Appendix A):
   * viewed with {@code FRBS_REPORT_VIEW}, exported with {@code FRBS_REPORT_EXPORT}, archived.
   *
   * @param code report code (e.g. {@code FRBS-MANCOM-MARKET})
   * @param title title
   * @param description one line purpose
   * @param parameters parameters
   * @return metadata in the BDOI report pack category
   */
  public static ReportMetadata frbs(
      String code, String title, String description, List<ParameterSpec> parameters) {
    return new ReportMetadata(
        code,
        title,
        ReportCategory.FRBS,
        description,
        parameters,
        Permission.FRBS_REPORT_VIEW,
        Permission.FRBS_REPORT_EXPORT,
        true);
  }
}
