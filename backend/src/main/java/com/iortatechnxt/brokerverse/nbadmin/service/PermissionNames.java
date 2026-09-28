package com.iortatechnxt.brokerverse.nbadmin.service;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Readable names of permission codes for users (notices, descriptions): the module prefix in words
 * and the rest in lower case, for example UAM_ENROLL becomes "User access enroll" and
 * PKG_TSU_APPROVE "Package TSU approve". The screens use the same rule (frontend permissionLabel).
 */
public final class PermissionNames {

  private static final Map<String, String> PREFIXES =
      Map.ofEntries(
          Map.entry("UAM", "User access"),
          Map.entry("SCR", "Screening"),
          Map.entry("RNW", "Renewal"),
          Map.entry("EB", "Employee benefits"),
          Map.entry("BCL", "Claims handling"),
          Map.entry("PKG", "Package"),
          Map.entry("MIG", "Data migration"),
          Map.entry("LOV", "Lists of values"),
          Map.entry("NB", "New business"));

  private static final Set<String> ACRONYMS =
      Set.of(
          "TSU", "MBS", "QS", "PRF", "KYC", "AML", "STR", "ACSL", "FRBS", "GL", "AR", "AP", "DV",
          "PR", "OR", "SOA", "EOD", "BIR", "VAT", "EWT", "SL", "SOD", "CSF", "ID", "LAMD", "MFA");

  private PermissionNames() {}

  /**
   * The readable name of a permission code.
   *
   * @param code permission code
   * @return name
   */
  public static String name(String code) {
    if (code == null || code.isBlank()) {
      return "";
    }
    String[] parts = code.split("_");
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      String word;
      if (i == 0 && PREFIXES.containsKey(part)) {
        word = PREFIXES.get(part);
      } else if (ACRONYMS.contains(part)) {
        word = part;
      } else {
        word = part.toLowerCase(Locale.ROOT);
      }
      if (!out.isEmpty()) {
        out.append(' ');
      }
      out.append(word);
    }
    return Character.toUpperCase(out.charAt(0)) + out.substring(1);
  }

  /**
   * Readable names of permission codes, separated by commas.
   *
   * @param codes permission codes
   * @return names
   */
  public static String names(Collection<String> codes) {
    return codes.stream().sorted().map(PermissionNames::name).collect(Collectors.joining(", "));
  }
}
