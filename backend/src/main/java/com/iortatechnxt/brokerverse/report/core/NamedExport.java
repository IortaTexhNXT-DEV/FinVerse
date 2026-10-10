package com.iortatechnxt.brokerverse.report.core;

/**
 * A report whose exported file is named by BDOI's convention {@code <name>_MMDDYYYY.<extension>}
 * (BDOI FRS FRUM.007.02 and FRUM.008.01; FRPM.007.01 and FRPM.021.01) when the setting {@value
 * ExportFileNames#SETTING} is on; otherwise the file is named by the report code.
 */
public interface NamedExport {

  /**
   * The constant part of the file name, for example {@code Audit Logs} or {@code User Access
   * Report}.
   *
   * @return name without the date and the extension
   */
  String exportName();
}
