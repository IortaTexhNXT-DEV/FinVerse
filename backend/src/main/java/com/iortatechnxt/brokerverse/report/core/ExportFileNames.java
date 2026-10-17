package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * File names of exported reports (conflicts C18 of User Access Maintenance and C23 of Product
 * Maintenance): with the setting {@value #SETTING} on (delivered), a report that is a {@link
 * NamedExport} is saved as {@code <name>_MMDDYYYY.<extension>} with the extraction date; otherwise,
 * and for every other report, as {@code <report code>.<extension>}.
 */
public final class ExportFileNames {

  /** The setting. */
  public static final String SETTING = "EXPORT_FILE_NAMES_BDOI";

  private static final DateTimeFormatter MMDDYYYY = DateTimeFormatter.ofPattern("MMddyyyy");

  private ExportFileNames() {}

  /**
   * The file name of an export.
   *
   * @param definition report
   * @param code report code
   * @param extension file extension without the dot
   * @param parameters business parameters (the setting)
   * @param clock clock (the extraction date)
   * @return file name
   */
  public static String of(
      ReportDefinition definition,
      String code,
      String extension,
      SystemParameterService parameters,
      Clock clock) {
    if (definition instanceof NamedExport named && bdoi(parameters)) {
      return dated(named.exportName(), BusinessClock.today(clock), extension);
    }
    return code + "." + extension;
  }

  /**
   * BDOI's file name {@code <name>_MMDDYYYY.<extension>}.
   *
   * @param name constant part
   * @param date extraction date
   * @param extension file extension without the dot
   * @return file name
   */
  public static String dated(String name, LocalDate date, String extension) {
    return name + "_" + MMDDYYYY.format(date) + "." + extension;
  }

  /**
   * Whether BDOI's file names apply.
   *
   * @param parameters business parameters
   * @return the setting, true when it is missing
   */
  public static boolean bdoi(SystemParameterService parameters) {
    return Boolean.parseBoolean(parameters.text(SETTING, "true").trim());
  }
}
