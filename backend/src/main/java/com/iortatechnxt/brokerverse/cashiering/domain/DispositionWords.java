package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import java.util.Map;

/**
 * The disposition types of unapplied payments as users read them in notices, history and the
 * approval inbox (REFUND is "refund to payor"): notices never show the type code.
 */
public final class DispositionWords {

  private static final Map<String, String> WORDS =
      Map.of(
          "APPLY_OTHER_INVOICE", "Apply to another invoice",
          "DST_APPLICATION", "Apply to DST only",
          "REFUND", "Refund to payor",
          "RECLASS", "Reclassify to another client",
          "TRANSFER_UNIT", "Transfer to another unit",
          "OTHERS", "Other (settled outside the system)",
          "HANDLING_FEE", "Handling fee income");

  private DispositionWords() {}

  /**
   * The words of a disposition type, sentence case.
   *
   * @param type type code, may be null
   * @return words, empty when null
   */
  public static String of(String type) {
    if (type == null) {
      return "";
    }
    String known = WORDS.get(type);
    if (known != null) {
      return known;
    }
    String words = DisplayFormat.words(type);
    return words.isEmpty() ? words : Character.toUpperCase(words.charAt(0)) + words.substring(1);
  }
}
