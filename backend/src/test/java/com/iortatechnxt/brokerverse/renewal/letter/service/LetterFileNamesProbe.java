package com.iortatechnxt.brokerverse.renewal.letter.service;

import com.iortatechnxt.brokerverse.renewal.domain.LetterType;
import com.iortatechnxt.brokerverse.renewal.domain.RaNotice;
import java.time.LocalDate;

/** Opens the file name rule of the letters to the tests of other packages. */
public final class LetterFileNamesProbe {

  private LetterFileNamesProbe() {}

  /**
   * The client's file name of a letter.
   *
   * @param product product prefix
   * @param type letter type
   * @param notice notice
   * @param reference reference number
   * @param day generation date
   * @return file name
   */
  public static String name(
      String product, LetterType type, RaNotice notice, String reference, LocalDate day) {
    return LetterFileNames.name(product, type, notice, reference, day);
  }
}
