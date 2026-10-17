package com.iortatechnxt.brokerverse.migration.intake.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.object.domain.MigDataObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The file contract of an extract (DATA_MIGRATION_DESIGN section 5.1): the data file name {@code
 * <LAYOUT>_<SOURCE>_<yyyyMMdd>_<nn>.csv|xlsx}, the object and source system it must belong to, and
 * the extract mode.
 */
final class ExtractNames {

  /** Delta extract mode. */
  static final String DELTA = "DELTA";

  private static final int G_LAYOUT = 1;
  private static final int G_SOURCE = 2;
  private static final int G_DAY = 3;
  private static final int G_SEQ = 4;

  private static final Pattern FILE_NAME =
      Pattern.compile("^([A-Z0-9]+)_([A-Z]+)_(\\d{8})_(\\d{2})\\.(CSV|XLSX)$");
  private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

  private ExtractNames() {}

  /**
   * Parses a data file name.
   *
   * @param fileName file name
   * @return its parts
   */
  static Name parse(String fileName) {
    Matcher m =
        FILE_NAME.matcher(fileName == null ? "" : fileName.strip().toUpperCase(Locale.ROOT));
    if (!m.matches()) {
      throw new BusinessRuleException(
          "MIG_FILE_NAME",
          "Name the file <LAYOUT>_<SOURCE>_<yyyyMMdd>_<nn>.csv or .xlsx,"
              + " for example F01C_EBIX_20271231_01.csv");
    }
    try {
      return new Name(
          m.group(G_LAYOUT),
          m.group(G_SOURCE),
          LocalDate.parse(m.group(G_DAY), DAY),
          Integer.parseInt(m.group(G_SEQ)));
    } catch (DateTimeParseException e) {
      throw new BusinessRuleException(
          "MIG_FILE_NAME", "The date in the file name is not a valid yyyyMMdd date", e);
    }
  }

  /**
   * The file belongs to the object chosen on the screen, the object is decided for migration or
   * archive, and the source system is one of the object's sources.
   *
   * @param object object of the layout
   * @param name parts of the file name
   * @param expectedObject object chosen on the screen, may be null
   */
  static void requireReceivable(MigDataObject object, Name name, String expectedObject) {
    if (expectedObject != null
        && !expectedObject.isBlank()
        && !expectedObject.equals(object.getCode())) {
      throw new BusinessRuleException(
          "MIG_FILE_OBJECT",
          "File layout "
              + name.layout()
              + " belongs to object "
              + object.getCode()
              + ", not "
              + expectedObject);
    }
    requireDecided(object);
    if (!object.sources().isEmpty() && !object.sources().contains(name.source())) {
      throw new BusinessRuleException(
          "MIG_FILE_SOURCE",
          "Source system " + name.source() + " is not a source of object " + object.getCode());
    }
  }

  private static void requireDecided(MigDataObject object) {
    if (!object.loadable() && !object.archive()) {
      throw new BusinessRuleException(
          "MIG_OBJECT_NOT_DECIDED",
          "Object "
              + object.getCode()
              + " is not decided for migration; extracts cannot be received");
    }
  }

  /**
   * The extract mode.
   *
   * @param mode requested mode
   * @return FULL or DELTA
   */
  static String mode(String mode) {
    return DELTA.equalsIgnoreCase(mode) ? DELTA : "FULL";
  }

  /**
   * The name the control file is read under (CSV unless an Excel file).
   *
   * @param controlName control file name
   * @return name
   */
  static String controlName(String controlName) {
    return controlName != null && controlName.toLowerCase(Locale.ROOT).endsWith(".xlsx")
        ? controlName
        : "control.csv";
  }

  /**
   * Parts of a file name.
   *
   * @param layout layout code
   * @param source source system
   * @param day as-of date
   * @param sequence sequence of the day
   */
  record Name(String layout, String source, LocalDate day, int sequence) {}
}
