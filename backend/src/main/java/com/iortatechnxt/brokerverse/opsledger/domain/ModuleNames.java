package com.iortatechnxt.brokerverse.opsledger.domain;

import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import java.util.Locale;
import java.util.Map;

/**
 * The business names of the teams and modules that own work on an invoice (lock owners, sources of
 * movements), as messages and notices name them: never the internal module code.
 */
public final class ModuleNames {

  private static final Map<String, String> NAMES =
      Map.ofEntries(
          Map.entry("ADJUSTMENT", "Adjustment"),
          Map.entry("BOOKING", "Booking"),
          Map.entry("CASHIERING", "Cashiering"),
          Map.entry("COLLECTIONS", "Collections"),
          Map.entry("COMMISSION", "Commission Receivables"),
          Map.entry("DISBURSEMENT", "Disbursement"),
          Map.entry("FRBS", "Service Fees"),
          Map.entry("OPSLEDGER", "Operations"),
          Map.entry("PAYREQUEST", "Payment Requests"),
          Map.entry("PRODRECON", "Production Reconciliation"),
          Map.entry("REMITTANCE", "Remittance"));

  private ModuleNames() {}

  /**
   * The name of a module from its code: PRODRECON is "Production Reconciliation".
   *
   * @param code module code, may be null
   * @return name, empty when null
   */
  public static String of(String code) {
    if (code == null) {
      return "";
    }
    String name = NAMES.get(code.toUpperCase(Locale.ROOT));
    if (name != null) {
      return name;
    }
    String words = DisplayFormat.words(code);
    return words.isEmpty() ? words : Character.toUpperCase(words.charAt(0)) + words.substring(1);
  }
}
