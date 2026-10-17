package com.iortatechnxt.brokerverse.renewal.placement.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * File names of the placement documents (FRRN.029.02.01, FRRN.029.02.02, FRRN.032.01): {@code
 * <InsurerCode>_<Product Line>_PlacementSlip_<Reference>_MMDDYYYY.pdf}, {@code
 * <InsurerCode>_<Product Line>_PlacementFile_MMDDYYYY.xlsx} and {@code
 * IA_<Reference>_MMDDYYYY.pdf}.
 */
public final class PlacementNames {

  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("MMddyyyy", Locale.ROOT);
  private static final String SEP = "_";

  private PlacementNames() {}

  /**
   * The name of a placement slip.
   *
   * @param insurerCode insurer
   * @param lineCode product line
   * @param ref renewal reference
   * @param date generation date
   * @return file name
   */
  public static String slip(String insurerCode, String lineCode, String ref, LocalDate date) {
    return prefix(insurerCode, lineCode)
        + "PlacementSlip_"
        + ref
        + SEP
        + DATE.format(date)
        + ".pdf";
  }

  /**
   * The name of a consolidated placement file.
   *
   * @param insurerCode insurer
   * @param lineCode product line
   * @param date generation date
   * @return file name
   */
  public static String file(String insurerCode, String lineCode, LocalDate date) {
    return prefix(insurerCode, lineCode) + "PlacementFile_" + DATE.format(date) + ".xlsx";
  }

  /**
   * The name of an Insurance Advice.
   *
   * @param ref renewal reference
   * @param date generation date
   * @return file name
   */
  public static String advice(String ref, LocalDate date) {
    return "IA_" + ref + SEP + DATE.format(date) + ".pdf";
  }

  private static String prefix(String insurerCode, String lineCode) {
    return insurerCode + SEP + (lineCode == null || lineCode.isBlank() ? "" : lineCode + SEP);
  }
}
