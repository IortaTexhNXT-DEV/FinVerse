package com.iortatechnxt.finverse.insurance;

import java.time.LocalDate;
import java.util.List;

/**
 * PORT implemented by the claims module: claims data needed by actuarial reserving (OSLR, IBNR
 * development triangles) and by reinsurance reporting.
 *
 * <p>Implementation contract: read-only, bulk queries, posted (approved) movements only.
 */
public interface ClaimsExperienceView {

  /**
   * Open claims with a non-zero outstanding reserve at a date.
   *
   * @param companyId company
   * @param asOf as-of date (movements dated on or before it)
   * @return outstanding claims
   */
  List<OutstandingClaim> outstanding(Long companyId, LocalDate asOf);

  /**
   * Posted movements in a date range, ordered by movement date.
   *
   * @param companyId company
   * @param from first movement date (inclusive)
   * @param to last movement date (inclusive)
   * @return movements
   */
  List<ClaimMovement> movements(Long companyId, LocalDate from, LocalDate to);
}
