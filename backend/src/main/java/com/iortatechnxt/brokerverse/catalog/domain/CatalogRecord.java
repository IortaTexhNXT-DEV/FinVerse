package com.iortatechnxt.brokerverse.catalog.domain;

/** Display facts of a catalog master record (approval inbox, audit trail). */
public interface CatalogRecord {

  /**
   * Business key, e.g. a risk code or "DST PROPERTY 2026-01-01".
   *
   * @return reference
   */
  String catalogReference();

  /**
   * One-line description.
   *
   * @return description
   */
  String catalogDescription();

  /**
   * Company of company-scoped records (insurers, sales organisation); null otherwise.
   *
   * @return company id or null
   */
  default Long catalogCompanyId() {
    return null;
  }
}
