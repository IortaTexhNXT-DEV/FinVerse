package com.iortatechnxt.brokerverse.productmaint.service;

import java.time.LocalDate;

/**
 * Port for the synchronisation of product master changes with other BDOI systems (BRPM.022
 * "synchronization of updates across all relevant systems"; PQ16 / Q08 parked: the target systems,
 * format and timing are not known). {@link ProductMasterFeedDefaults} registers an adapter that
 * only logs the change until an integration adapter is supplied as a bean.
 */
public interface ProductMasterFeed {

  /**
   * A package version was released or a package retired or expired.
   *
   * @param change what changed
   */
  void publish(ProductMasterChange change);

  /**
   * A product master change.
   *
   * @param productCode risk code
   * @param versionNo version concerned, null for a retirement
   * @param kind RELEASED, RETIRED or EXPIRED
   * @param effectiveDate date the change applies from
   * @param sourceRequestNo package request, null when none
   */
  record ProductMasterChange(
      String productCode,
      Integer versionNo,
      String kind,
      LocalDate effectiveDate,
      String sourceRequestNo) {}
}
