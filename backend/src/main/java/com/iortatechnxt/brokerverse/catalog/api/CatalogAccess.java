package com.iortatechnxt.brokerverse.catalog.api;

/** Security expressions of the catalog endpoints. */
final class CatalogAccess {

  /** Reading the catalog: master data viewers and the broking teams that use it. */
  static final String READ =
      "hasAnyAuthority('MASTER_VIEW', 'ACCOUNT_VIEW', 'QUOTE_VIEW', 'TSU_PROCESS')";

  /** Maintaining catalog records (maker). */
  static final String MAINTAIN = "hasAuthority('MASTER_MAINTAIN')";

  /** Authorizing catalog records (checker). */
  static final String AUTHORIZE = "hasAuthority('MASTER_AUTHORIZE')";

  private CatalogAccess() {}
}
