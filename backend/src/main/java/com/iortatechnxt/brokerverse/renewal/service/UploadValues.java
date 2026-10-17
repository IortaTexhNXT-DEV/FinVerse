package com.iortatechnxt.brokerverse.renewal.service;

import java.util.Comparator;

/** Matching of values typed in upload files against codes and labels, ignoring the case. */
public final class UploadValues {

  private static final Comparator<String> IGNORE_CASE = String.CASE_INSENSITIVE_ORDER;

  private UploadValues() {}

  /**
   * Whether a typed value names a code or a label: case and the space or underscore between words
   * do not matter.
   *
   * @param typed typed value
   * @param code code
   * @param label label, may be null
   * @return true when it names them
   */
  public static boolean names(String typed, String code, String label) {
    if (typed == null) {
      return false;
    }
    String text = typed.strip();
    return IGNORE_CASE.compare(text.replace(' ', '_'), code) == 0
        || label != null && IGNORE_CASE.compare(text, label) == 0;
  }
}
