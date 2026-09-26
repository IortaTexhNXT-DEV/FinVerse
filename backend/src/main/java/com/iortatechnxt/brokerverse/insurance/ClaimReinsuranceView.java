package com.iortatechnxt.brokerverse.insurance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * PORT implemented by the reinsurance module: the reinsurers' share of outstanding claim reserves,
 * used by actuarial reserving to report reserves net of reinsurance.
 *
 * <p>Implementation contract: read-only, bulk query; claims without cession may be omitted.
 */
public interface ClaimReinsuranceView {

  /**
   * Reinsurers' share of the outstanding reserve of each claim at a date.
   *
   * @param companyId company
   * @param asOf as-of date
   * @return base-currency amount by claim id
   */
  Map<Long, BigDecimal> reinsuranceShareOfOutstanding(Long companyId, LocalDate asOf);
}
