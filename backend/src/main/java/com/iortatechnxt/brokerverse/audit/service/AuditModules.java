package com.iortatechnxt.brokerverse.audit.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The module of an audit entry, named from its record type (BDOI FRS FRUM.008.01: Module). The
 * first rule that matches the record type applies; a record type without a rule shows in words.
 */
public final class AuditModules {

  /** Name of the User Access Maintenance module. */
  public static final String USER_ACCESS = "User Access Maintenance";

  /** Name of the Product Maintenance module. */
  public static final String PRODUCT_MAINTENANCE = "Product Maintenance";

  /** Record types of Product Maintenance (its Audit Logs). */
  public static final List<String> PRODUCT_MAINTENANCE_TYPES =
      List.of(
          "PackageRequest",
          "PackageDeactivation",
          "Product",
          "ProductVersion",
          "IncentiveCriteria",
          "Insurer",
          "Coverage",
          "Clause",
          "RateTable",
          "RateSchemeException",
          "FieldRule",
          "PackageAdvisory",
          "PlacementUpdateReport");

  private static final Set<String> USER_ACCESS_TYPES =
      Set.of(
          "AccessRequest",
          "AccessBatch",
          "AppUser",
          "user",
          "Role",
          "SodRule",
          "PermissionConflict",
          "IdentityEvent",
          "SystemParameter",
          "RetentionRule",
          "NotificationPreference");

  /** Modules by the beginnings of their record types, tried in this order. */
  private static final Map<String, List<String>> PREFIXES = new LinkedHashMap<>();

  static {
    PREFIXES.put("Data Migration", List.of("Mig"));
    PREFIXES.put("Employee Benefits", List.of("Eb"));
    PREFIXES.put("Renewal", List.of("Renewal", "Rnw"));
    PREFIXES.put("Screening", List.of("Screening", "Watchlist"));
    PREFIXES.put("Submitted Business", List.of("Submitted", "Sbm"));
    PREFIXES.put("Remittance", List.of("Remittance"));
    PREFIXES.put("Production Reconciliation", List.of("Recon"));
    PREFIXES.put("Account Statements", List.of("Acsl"));
    PREFIXES.put("Claims", List.of("BrokerClaim"));
    PREFIXES.put("Collections", List.of("Collection"));
    PREFIXES.put("Cashiering", List.of("Cash", "Receipt", "DepositSlip", "Pdc", "PostDated"));
    PREFIXES.put(
        "Disbursement", List.of("Payment", "Disbursement", "Payee", "Liquidation", "PettyCash"));
    PREFIXES.put(
        "General Ledger", List.of("Journal", "Gl", "FiscalYear", "Period", "Budget", "Coa"));
    PREFIXES.put("Tax", List.of("Tax", "Bir"));
    PREFIXES.put(
        "New Business",
        List.of(
            "Proposal",
            "Quotation",
            "Placement",
            "Endorsement",
            "Booked",
            "Booking",
            "HoldCover",
            "InsuranceAdvice",
            "Epolicy",
            "Account"));
    PREFIXES.put("Client Management", List.of("Client"));
    PREFIXES.put("Reports", List.of("Report"));
  }

  private AuditModules() {}

  /**
   * The module of a record type.
   *
   * @param entityType record type of the entry
   * @return module name
   */
  public static String of(String entityType) {
    if (entityType == null || entityType.isBlank()) {
      return "General";
    }
    if (USER_ACCESS_TYPES.contains(entityType)) {
      return USER_ACCESS;
    }
    if (PRODUCT_MAINTENANCE_TYPES.contains(entityType)) {
      return PRODUCT_MAINTENANCE;
    }
    return PREFIXES.entrySet().stream()
        .filter(e -> e.getValue().stream().anyMatch(entityType::startsWith))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElseGet(() -> words(entityType));
  }

  /**
   * A record type in words ("JournalBatch" becomes "Journal Batch").
   *
   * @param entityType record type
   * @return words
   */
  public static String words(String entityType) {
    return entityType.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ');
  }
}
