package com.iortatechnxt.brokerverse.catalog.api;

/** Security expressions of the catalog endpoints. */
final class CatalogAccess {

  /** Reading the catalog: master data viewers, Product Maintenance and the broking teams. */
  static final String READ =
      "hasAnyAuthority('MASTER_VIEW', 'PRODUCT_VIEW', 'ACCOUNT_VIEW', 'QUOTE_VIEW', 'TSU_PROCESS')";

  /** Maintaining catalog records (maker) outside the product areas. */
  static final String MAINTAIN = "hasAuthority('MASTER_MAINTAIN')";

  /** Maintaining the product areas: products, lines, rules (maker; MBS or master data). */
  static final String MAINTAIN_PRODUCTS = "hasAnyAuthority('MASTER_MAINTAIN', 'PRODUCT_MAINTAIN')";

  /** Package set-up, coverages and clauses (MBS, PMADD05). */
  static final String PRODUCT_MAINTAIN = "hasAuthority('PRODUCT_MAINTAIN')";

  /** Post-set-up validation of package versions (PMADD06). */
  static final String VALIDATE = "hasAuthority('PRODUCT_VALIDATE')";

  /** Incentive criteria (PMADD07). */
  static final String INCENTIVES = "hasAuthority('INCENTIVE_CRITERIA_MAINTAIN')";

  /** Requesting a rate-scheme exception for a quotation or account (BRPM.007). */
  static final String REQUEST_EXCEPTION =
      "hasAnyAuthority('QUOTE_MAINTAIN', 'ACCOUNT_MAINTAIN', 'PRODUCT_MAINTAIN')";

  /** Authorizing catalog records (checker; the permission per kind is checked by the service). */
  static final String AUTHORIZE = "hasAnyAuthority('MASTER_AUTHORIZE', 'PRODUCT_AUTHORIZE')";

  /** Deactivating catalog records (the permission per kind is checked by the service). */
  static final String DEACTIVATE =
      "hasAnyAuthority('MASTER_MAINTAIN', 'PRODUCT_MAINTAIN', 'INCENTIVE_CRITERIA_MAINTAIN',"
          + " 'PRODUCT_AUTHORIZE')";

  private CatalogAccess() {}
}
