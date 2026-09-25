package com.iortatechnxt.brokerverse.catalog.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * What to rate: a product, optionally with the insurer branch (LGT, commission), the items and the
 * period.
 *
 * @param companyId company (insurer lookups)
 * @param productCode risk code
 * @param insurerCode insurer party code, may be null before placement
 * @param branchCode insurer branch (LGT), may be null
 * @param items items to rate
 * @param multiYear multi-year cover (BRNB.112)
 * @param basis period basis; null for annual
 * @param periodFrom period start
 * @param periodTo period end
 * @param commissionRate commission override in percent, null for the insurer / product rate
 * @param endorsement endorsement transaction (no minimum premium)
 * @param ratingDate date of the rates; null for the period start (or today)
 */
public record RatingQuery(
    Long companyId,
    String productCode,
    String insurerCode,
    String branchCode,
    List<Item> items,
    boolean multiYear,
    PeriodBasis basis,
    LocalDate periodFrom,
    LocalDate periodTo,
    BigDecimal commissionRate,
    boolean endorsement,
    LocalDate ratingDate) {

  /** Defensive copy. */
  public RatingQuery {
    items = items == null ? List.of() : List.copyOf(items);
  }

  /**
   * Why a transaction is rated, which decides the package version that prices it (BRPM.007,
   * PRODUCT_MAINTENANCE_DESIGN section 5.2). Non-packaged products are rated the same way for every
   * purpose. Until the Renewal BRD gives accounts a business type, quotations and accounts rate as
   * NEW_BUSINESS and endorsements as ENDORSEMENT; RENEWAL is the seam.
   */
  public enum Purpose {
    /** The current released version on the transaction date, whatever the period start. */
    NEW_BUSINESS,
    /** The given version when released or superseded (not expired), else the current one. */
    RENEWAL,
    /** The version of the original account (mandatory). */
    ENDORSEMENT
  }

  /**
   * One item to rate.
   *
   * @param label label (plate number, address, description)
   * @param sumInsured sum insured
   * @param ratePercent premium rate in percent; null for the product's default rate
   * @param biLimit motor excess BI limit, null for none
   * @param pdLimit motor PD limit, null for none
   */
  public record Item(
      String label,
      BigDecimal sumInsured,
      BigDecimal ratePercent,
      BigDecimal biLimit,
      BigDecimal pdLimit) {}
}
