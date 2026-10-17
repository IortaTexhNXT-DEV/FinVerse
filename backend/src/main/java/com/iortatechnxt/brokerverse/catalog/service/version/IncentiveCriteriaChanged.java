package com.iortatechnxt.brokerverse.catalog.service.version;

import java.time.LocalDate;

/**
 * An incentive criterion on the maintained products matrix changed state (PMADD07/08). Published by
 * the catalog <b>after commit</b> of the authorisation (or deactivation); booking and the
 * Operations commission module may refresh what they cache, and nothing else depends on it.
 *
 * @param companyId company of the criterion
 * @param code criterion code (for example CPC2)
 * @param change what happened
 * @param effectiveFrom effective start of the row concerned
 * @param effectiveTo effective end, null when open-ended
 */
public record IncentiveCriteriaChanged(
    Long companyId, String code, Change change, LocalDate effectiveFrom, LocalDate effectiveTo) {

  /** What happened to the criterion. */
  public enum Change {
    /** A new criterion was authorised and is ACTIVE. */
    ACTIVATED,
    /** An active criterion was end-dated and its successor row authorised. */
    AMENDED,
    /** A criterion was deactivated (effective end set, INACTIVE). */
    DEACTIVATED
  }
}
