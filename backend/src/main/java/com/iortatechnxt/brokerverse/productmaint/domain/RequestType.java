package com.iortatechnxt.brokerverse.productmaint.domain;

/**
 * What a package request asks for (BRPM.011, list PKG_REQUEST_TYPE; the difference between AMEND
 * and UPDATE is PQ13). Every type except NEW works on an existing packaged product.
 */
public enum RequestType {
  /** A new package: the MBS set-up creates the product and its first version. */
  NEW("New package"),
  /** Changed commercial terms of an existing package (new version). */
  AMEND("Amendment"),
  /** Updated package details (new version). */
  UPDATE("Update"),
  /** Renewal of a package reaching its end date (BRPM.017); negotiation is optional. */
  RENEW("Renewal"),
  /** Retirement of a package ("deletion", BRPM.011 / BRPM.006): no negotiation, no version. */
  RETIRE("Retirement"),
  /** Reactivation of an expired package through a new version (BRPM.006). */
  REACTIVATE("Reactivation");

  private final String label;

  RequestType(String label) {
    this.label = label;
  }

  /**
   * The type as users read it (documents, messages).
   *
   * @return label
   */
  public String label() {
    return label;
  }

  /**
   * Whether the request works on an existing product.
   *
   * @return true for every type but NEW
   */
  public boolean needsProduct() {
    return this != NEW;
  }

  /**
   * Whether insurer negotiation is required by default (RETIRE never negotiates; a renewal may keep
   * its terms).
   *
   * @return default of the negotiation flag
   */
  public boolean negotiatesByDefault() {
    return this != RETIRE && this != RENEW;
  }
}
