package com.iortatechnxt.brokerverse.common.exception;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Business names of records in user messages ("Auto-booking rule not found: 7"): a class-style name
 * (AutoBookRule, ServiceInvoiceType, PackageRequest work item) or a code (EB_MEMBER) is written in
 * words, with the agreed names of the short forms; a name already in words is kept as it is.
 */
public final class ResourceNames {

  /** Records whose name in words is not the plain reading of their class name. */
  private static final Map<String, String> NAMES =
      Map.ofEntries(
          Map.entry("AutoBookRule", "Auto-booking rule"),
          Map.entry("WorkCase", "Work item"),
          Map.entry("StoredFile", "File"),
          Map.entry("StageRow", "Staged row"),
          Map.entry("BrokerClaim", "Claim"),
          Map.entry("BrokerClaimDiary", "Claim diary entry"),
          Map.entry("BrokerClaimStatusAccess", "Claim status access"),
          Map.entry("AccountLegacyHeader", "Legacy account header"),
          Map.entry("MigDataObject", "Data object"),
          Map.entry("LovType", "List type"),
          Map.entry("LovValue", "List value"),
          Map.entry("EbBor", "Broker of record letter"),
          Map.entry("DpList", "Direct payment list"),
          Map.entry("OpsHandoff", "Operations hand-off"));

  /** Short forms inside a class name, in words (the first word takes a capital letter). */
  private static final Map<String, String> WORDS =
      Map.ofEntries(
          Map.entry("Eb", "employee benefits"),
          Map.entry("Mig", "migration"),
          Map.entry("Dp", "direct payment"),
          Map.entry("Ops", "operations"),
          Map.entry("Recon", "reconciliation"),
          Map.entry("Soa", "statement of account"),
          Map.entry("Fac", "facultative"),
          Map.entry("Sod", "separation of duties"),
          Map.entry("Lov", "list"),
          Map.entry("Fx", "FX"),
          Map.entry("Gl", "GL"),
          Map.entry("Sl", "SL"),
          Map.entry("Pdc", "PDC"),
          Map.entry("Bir", "BIR"),
          Map.entry("Cwt", "CWT"),
          Map.entry("Ic", "IC"),
          Map.entry("Tsu", "TSU"),
          Map.entry("Acsl", "ACSL"),
          Map.entry("Dppr", "DPPR"),
          Map.entry("Bor", "BOR"));

  private ResourceNames() {}

  /**
   * The name of a record as users read it.
   *
   * @param resource record name as the code has it, may be null
   * @return name in words
   */
  public static String of(String resource) {
    if (resource == null || resource.isBlank()) {
      return "Record";
    }
    StringBuilder out = new StringBuilder();
    for (String token : resource.strip().split(" ")) {
      if (!out.isEmpty()) {
        out.append(' ');
      }
      out.append(word(token));
    }
    String text = out.toString();
    return Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

  private static String word(String token) {
    String known = NAMES.get(token);
    if (known != null) {
      return known;
    }
    List<String> parts = new ArrayList<>();
    if (codeStyle(token)) {
      for (String part : token.split("_")) {
        String lower = part.toLowerCase(Locale.ROOT);
        parts.add(Character.toUpperCase(lower.charAt(0)) + lower.substring(1));
      }
    } else if (classStyle(token)) {
      int start = 0;
      for (int i = 1; i < token.length(); i++) {
        if (Character.isUpperCase(token.charAt(i))) {
          parts.add(token.substring(start, i));
          start = i;
        }
      }
      parts.add(token.substring(start));
    } else {
      return token;
    }
    return String.join(
        " ", parts.stream().map(p -> WORDS.getOrDefault(p, p.toLowerCase(Locale.ROOT))).toList());
  }

  /** AutoBookRule: letters and digits, a capital first, lower case letters and further capitals. */
  private static boolean classStyle(String token) {
    if (token.length() < 2 || !Character.isUpperCase(token.charAt(0))) {
      return false;
    }
    int capitals = 0;
    boolean lower = false;
    for (int i = 0; i < token.length(); i++) {
      char c = token.charAt(i);
      if (!Character.isLetterOrDigit(c) || c > 'z') {
        return false;
      }
      capitals += Character.isUpperCase(c) ? 1 : 0;
      lower |= Character.isLowerCase(c);
    }
    return lower && capitals > 1 && !Character.isUpperCase(token.charAt(1));
  }

  /** EB_MEMBER: capitals and digits in parts joined by single underscores. */
  private static boolean codeStyle(String token) {
    if (!token.contains("_") || token.contains("__") || !Character.isLetter(token.charAt(0))) {
      return false;
    }
    return !token.endsWith("_") && token.chars().allMatch(ResourceNames::codeChar);
  }

  private static boolean codeChar(int c) {
    return c == '_' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
  }
}
