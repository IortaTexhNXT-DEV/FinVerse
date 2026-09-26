package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
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
 *     <p>Every report exports to PDF, Excel, ODS, CSV and XML. A {@link #documentStyle} report (a
 *     document or schedule, such as the board schedules BDOI asks for in Word, A1-FRBS) also offers
 *     Word in the export menus (client requirement 16); declare it with {@link #asDocument()}. The
 *     renderer itself accepts Word for any report (batches, API).
 * @param archived whether runs and exports are archived
 * @param documentStyle whether the report is a document or schedule, also exported to Word
 */
public record ReportMetadata(
    String code,
    String title,
    ReportCategory category,
    String description,
    List<ParameterSpec> parameters,
    Permission permission,
    Permission exportPermission,
    boolean archived,
    boolean documentStyle) {

  /** Canonical constructor copying the parameter list; export defaults to the view permission. */
  public ReportMetadata {
    parameters = List.copyOf(parameters);
    exportPermission = exportPermission == null ? permission : exportPermission;
  }

  /**
   * A tabular report (not a document): PDF, Excel, ODS, CSV and XML.
   *
   * @param code report code
   * @param title title
   * @param category menu group
   * @param description one line purpose
   * @param parameters parameters
   * @param permission permission required to run (view)
   * @param exportPermission permission required to export
   * @param archived whether runs and exports are archived
   */
  @SuppressWarnings("java:S107") // the record components of a catalogue entry
  public ReportMetadata(
      String code,
      String title,
      ReportCategory category,
      String description,
      List<ParameterSpec> parameters,
      Permission permission,
      Permission exportPermission,
      boolean archived) {
    this(
        code,
        title,
        category,
        description,
        parameters,
        permission,
        exportPermission,
        archived,
        false);
  }

  /**
   * The same report declared as a document or schedule, exported to Word as well.
   *
   * @return metadata with {@code documentStyle}
   */
  public ReportMetadata asDocument() {
    return new ReportMetadata(
        code,
        title,
        category,
        description,
        parameters,
        permission,
        exportPermission,
        archived,
        true);
  }

  /**
   * The formats offered in the export menus: Word only for documents and schedules.
   *
   * @return formats in menu order
   */
  public List<ExportFormat> formats() {
    List<ExportFormat> formats = new ArrayList<>(List.of(ExportFormat.values()));
    if (!documentStyle) {
      formats.remove(ExportFormat.DOCX);
    }
    return List.copyOf(formats);
  }

  /**
   * Whether the export menus offer a format for the report.
   *
   * @param format format
   * @return true when offered
   */
  public boolean offers(ExportFormat format) {
    return format != ExportFormat.DOCX || documentStyle;
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
   * A Compliance report (SANCTION_SCREENING_DESIGN section 11.1, SNSRP-901 / 903): viewed and
   * exported with {@code SCR_REPORT_VIEW}, archived.
   *
   * @param code report code (e.g. {@code SCR-CASE-STATUS})
   * @param title title
   * @param description one line purpose
   * @param parameters parameters
   * @return metadata in the Compliance category
   */
  public static ReportMetadata compliance(
      String code, String title, String description, List<ParameterSpec> parameters) {
    return new ReportMetadata(
        code,
        title,
        ReportCategory.COMPLIANCE,
        description,
        parameters,
        Permission.SCR_REPORT_VIEW,
        Permission.SCR_REPORT_VIEW,
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

  /**
   * A Claims Handling report of the broking claims module (CLAIMS_BROKING_DESIGN section 10,
   * BRCLM.026-034/038/040): viewed with {@code BCL_REPORT_VIEW}, exported with {@code
   * BCL_REPORT_EXPORT}, archived. The data extract ({@code BCL-DATA-EXTRACT}) builds its own
   * metadata with {@code BCL_DATA_EXTRACT} for both.
   *
   * @param code report code (e.g. {@code BCL-OUTSTANDING})
   * @param title title
   * @param description one line purpose
   * @param parameters parameters
   * @return metadata in the Claims Handling category
   */
  public static ReportMetadata claimsHandling(
      String code, String title, String description, List<ParameterSpec> parameters) {
    return new ReportMetadata(
        code,
        title,
        ReportCategory.CLAIMS_HANDLING,
        description,
        parameters,
        Permission.BCL_REPORT_VIEW,
        Permission.BCL_REPORT_EXPORT,
        true);
  }

  /**
   * An Employee Benefits report (EMPLOYEE_BENEFITS_DESIGN section 9, BRID-022, 022.01, 030): viewed
   * and exported with {@code EB_REPORT_VIEW}, archived. Reports that are documents add {@link
   * #asDocument()} for the Word export.
   *
   * @param code report code (e.g. {@code EB-PRODUCTION})
   * @param title title
   * @param description one line purpose
   * @param parameters parameters
   * @return metadata in the Employee Benefits category
   */
  public static ReportMetadata employeeBenefits(
      String code, String title, String description, List<ParameterSpec> parameters) {
    return new ReportMetadata(
        code,
        title,
        ReportCategory.EMPLOYEE_BENEFITS,
        description,
        parameters,
        Permission.EB_REPORT_VIEW,
        Permission.EB_REPORT_VIEW,
        true);
  }
}
