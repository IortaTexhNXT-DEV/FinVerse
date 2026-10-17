package com.iortatechnxt.brokerverse.renewal.check.service;

import java.util.Map;

/** Names of the renewal checks shown to users (lists, record page, reports, letters). */
public final class CheckNames {

  private static final Map<String, String> NAMES =
      Map.ofEntries(
          Map.entry("REFERENCE_MATCH", "Reference match"),
          Map.entry("PN_PRESENT", "PN number present"),
          Map.entry("RISK_CODE_RENEWABLE", "Risk code renewable"),
          Map.entry("LAMD_STATUS", "Loan status"),
          Map.entry("CLAIMS", "Claims"),
          Map.entry("ENDORSEMENT_PENDING", "Endorsement in progress"),
          Map.entry("OUTSTANDING_PREMIUM", "Outstanding premium"),
          Map.entry("FINANCIAL_IMPACT", "Financial impact"),
          Map.entry("INSURER_RESPONSE_MATCH", "Insurer response match"),
          Map.entry("MANDATORY_FIELDS", "Mandatory fields"),
          Map.entry("PRODUCT_RENEWABLE", "Product renewable"),
          Map.entry("INSURER_USABLE", "Insurer usable"),
          Map.entry("DUPLICATE_CANDIDATE", "Duplicate renewal"),
          Map.entry("KYC_DUE", "KYC review due"),
          Map.entry("PACKAGE_REMAP", "Package of a migrated policy"),
          Map.entry("TSI_THRESHOLD", "Total sum insured threshold"),
          Map.entry("RISK_CODE_DEFINED", "Risk code defined"),
          Map.entry("INSURER_RENEWABLE_LIST", "Insurer renewable list"),
          Map.entry("TOTAL_LOSS", "Total loss claim"),
          Map.entry("BDOFC_SOLD", "BDOFC or BDOSOLD report"),
          Map.entry("INSURER_DISPOSITION", "Insurer disposition"),
          Map.entry("DUPLICATE_ACCOUNT", "Potential duplicate account"));

  private CheckNames() {}

  /**
   * The name of a check.
   *
   * @param code check code
   * @return name, the code when unknown
   */
  public static String of(String code) {
    return code == null ? null : NAMES.getOrDefault(code, code);
  }
}
