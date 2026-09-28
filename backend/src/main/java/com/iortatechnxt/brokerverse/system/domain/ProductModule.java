package com.iortatechnxt.brokerverse.system.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The product modules of iNXT BrokerVerse that a deployment may switch off. A module is made of
 * top-level code packages (its controllers, reports and jobs) and, where a module shares a package
 * with another one, of the permissions it owns alone ({@link #ownPermissions()}).
 *
 * <p>The platform (security, administration, general ledger, reporting engine, master data,
 * workflow, messaging and the other shared services) belongs to no product module and is always on.
 * Which modules a deployment uses is data: the switches in {@code sys_product_module}, maintained
 * by the system administrator with a second approval, and the module profiles of {@code
 * sys_module_profile} (V1160).
 */
public enum ProductModule {
  /**
   * Client management, quotations, accounts, proposals, placement, issuance, booking, NB reports.
   */
  NEW_BUSINESS(
      "New Business",
      List.of(
          "crm",
          "quotation",
          "account",
          "nonpackage",
          "placement",
          "issuance",
          "booking",
          "nbreport"),
      List.of(),
      List.of()),
  /** Package requests and the product maintenance workflow (the catalogue stays platform). */
  PRODUCT_MAINTENANCE("Product Maintenance", List.of("productmaint"), List.of(), List.of()),
  /** Invoice ledger, cashiering, remittance, adjustment, production reconciliation, commission. */
  OPERATIONS(
      "Operations",
      List.of("opsledger", "cashiering", "remittance", "adjustment", "prodrecon", "commission"),
      List.of(),
      List.of()),
  /** Collections and billing follow-up. */
  COLLECTIONS("Collections", List.of("collections"), List.of(), List.of()),
  /** Disbursement, requests for payment, ACSL and FRBS schedules. */
  ACCOUNTING_DISBURSEMENT(
      "Accounting, Disbursement and ACSL",
      List.of("disbursement", "payrequest", "acsl", "frbs"),
      List.of(),
      List.of()),
  /** Renewal processing. */
  RENEWAL("Renewal", List.of("renewal"), List.of(), List.of()),
  /** Claims handling of the broker. */
  CLAIMS_HANDLING("Claims Handling", List.of("brokerclaims"), List.of(), List.of()),
  /** Employee benefits programmes. */
  EMPLOYEE_BENEFITS("Employee Benefits", List.of("eb"), List.of(), List.of()),
  /** Customer service facility of the contact centre. */
  CUSTOMER_SERVICING("Customer Service Facility", List.of("csf"), List.of(), List.of()),
  /** Sanction screening and risk profiling. */
  SANCTION_SCREENING("Sanction Screening", List.of("screening"), List.of(), List.of()),
  /** Submitted policies. */
  SUBMITTED_POLICIES("Submitted Policies", List.of("submitted"), List.of(), List.of()),
  /** Data migration from the legacy system. */
  DATA_MIGRATION("Data Migration", List.of("migration"), List.of(), List.of()),
  /** Supplier invoices, payment vouchers, bank accounts, cheque books, petty cash. */
  PAYABLES("Payables and Cash", List.of("payables"), List.of(), List.of()),
  /** Receipts, deposits, bank statements and bank reconciliation. */
  RECEIVABLES("Receivables and Banking", List.of("receivables"), List.of(), List.of()),
  /** Fixed assets and investments. */
  ASSETS_INVESTMENTS(
      "Assets and Investments", List.of("fixedasset", "investment"), List.of(), List.of()),
  /** Budgets and budget against actual. */
  BUDGET("Budgets", List.of("budget"), List.of(), List.of()),
  /** Tax and statutory returns. */
  TAX_STATUTORY("Tax and Statutory", List.of("tax"), List.of(), List.of()),
  /** Insurer suite: insurer products, quotations and policies. */
  UNDERWRITING("Underwriting (insurer)", List.of("underwriting"), List.of(), List.of()),
  /** Insurer suite: claims of an insurer with reserves and settlements. */
  INSURER_CLAIMS("Claims (insurer)", List.of("claims"), List.of(), List.of("UNDERWRITING")),
  /** Insurer suite: treaties, cessions and facultative placements. */
  REINSURANCE("Reinsurance (insurer)", List.of("reinsurance"), List.of(), List.of("UNDERWRITING")),
  /** Insurer suite: technical reserves. */
  ACTUARIAL_RESERVES(
      "Actuarial Reserves (insurer)",
      List.of("reserves", "insurance"),
      List.of(),
      List.of("UNDERWRITING", "INSURER_CLAIMS")),
  /** Group consolidation and inter-company. */
  CONSOLIDATION("Consolidation", List.of("consolidation"), List.of(), List.of()),
  /** Insurer suite: premium tax, documentary stamp tax and the insurer IC schedules. */
  INSURER_TAX(
      "Insurer Tax Schedules",
      List.of(),
      List.of("INSURER_TAX_VIEW"),
      List.of("TAX_STATUTORY", "UNDERWRITING"));

  private static final String ROOT = "com.iortatechnxt.brokerverse.";

  private final String displayName;
  private final List<String> packages;
  private final List<String> ownPermissions;
  private final List<String> dependsOn;

  ProductModule(
      String displayName,
      List<String> packages,
      List<String> ownPermissions,
      List<String> dependsOn) {
    this.displayName = displayName;
    this.packages = packages;
    this.ownPermissions = ownPermissions;
    this.dependsOn = dependsOn;
  }

  /**
   * Name shown to users.
   *
   * @return name
   */
  public String displayName() {
    return displayName;
  }

  /**
   * Top-level code packages of the module.
   *
   * @return package names below the root package
   */
  public List<String> packages() {
    return packages;
  }

  /**
   * Permissions owned by the module although its screens are in a package of another module.
   *
   * @return permission codes
   */
  public List<String> ownPermissions() {
    return ownPermissions;
  }

  /**
   * Modules that must be on while this one is on.
   *
   * @return module codes
   */
  public List<String> dependsOn() {
    return dependsOn;
  }

  /**
   * The module a class belongs to.
   *
   * @param type class (a controller, report or job)
   * @return the module, empty for a platform class
   */
  public static Optional<ProductModule> ofClass(Class<?> type) {
    return ofPackage(type.getName());
  }

  /**
   * The module of a fully qualified class or package name.
   *
   * @param qualifiedName class or package name
   * @return the module, empty for the platform
   */
  public static Optional<ProductModule> ofPackage(String qualifiedName) {
    if (qualifiedName == null || !qualifiedName.startsWith(ROOT)) {
      return Optional.empty();
    }
    String rest = qualifiedName.substring(ROOT.length());
    int dot = rest.indexOf('.');
    String top = dot < 0 ? rest : rest.substring(0, dot);
    return Arrays.stream(values()).filter(m -> m.packages.contains(top)).findFirst();
  }

  /**
   * The module that owns a permission explicitly.
   *
   * @param permission permission code
   * @return module, empty when no module declares it
   */
  public static Optional<ProductModule> owningPermission(String permission) {
    return Arrays.stream(values()).filter(m -> m.ownPermissions.contains(permission)).findFirst();
  }

  /**
   * Finds a module by code.
   *
   * @param code module code
   * @return module, empty for an unknown code
   */
  public static Optional<ProductModule> byCode(String code) {
    return Arrays.stream(values()).filter(m -> m.name().equals(code)).findFirst();
  }
}
