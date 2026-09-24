package com.iortatechnxt.brokerverse.reserves.service;

import com.iortatechnxt.brokerverse.insurance.ClaimMovement;
import com.iortatechnxt.brokerverse.insurance.ClaimReinsuranceView;
import com.iortatechnxt.brokerverse.insurance.ClaimsExperienceView;
import com.iortatechnxt.brokerverse.insurance.OutstandingClaim;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Optional access to the insurance shared kernel ports implemented by the claims and reinsurance
 * modules. Reserving never depends on those modules: when one is not deployed the accessor returns
 * empty data, so OSLR, chain-ladder IBNR and the reinsurers' share of claims are zero.
 */
@Component
public class ReservePorts {

  private final ObjectProvider<ClaimsExperienceView> claims;
  private final ObjectProvider<ClaimReinsuranceView> reinsurance;

  /**
   * Creates the accessor.
   *
   * @param claims claims experience view, if the claims module is deployed
   * @param reinsurance reinsurance share of claims, if the reinsurance module is deployed
   */
  public ReservePorts(
      ObjectProvider<ClaimsExperienceView> claims,
      ObjectProvider<ClaimReinsuranceView> reinsurance) {
    this.claims = claims;
    this.reinsurance = reinsurance;
  }

  /**
   * Whether claims data is available.
   *
   * @return true when a claims view is deployed
   */
  public boolean hasClaims() {
    return claims.getIfAvailable() != null;
  }

  /**
   * Whether the reinsurers' share of claims is available.
   *
   * @return true when a reinsurance view is deployed
   */
  public boolean hasClaimReinsurance() {
    return reinsurance.getIfAvailable() != null;
  }

  /**
   * Open claims at a date.
   *
   * @param companyId company
   * @param asOf date
   * @return outstanding claims, empty without claims module
   */
  public List<OutstandingClaim> outstanding(Long companyId, LocalDate asOf) {
    ClaimsExperienceView view = claims.getIfAvailable();
    return view == null ? List.of() : view.outstanding(companyId, asOf);
  }

  /**
   * Posted claim movements in a date range.
   *
   * @param companyId company
   * @param from first date
   * @param to last date
   * @return movements, empty without claims module
   */
  public List<ClaimMovement> movements(Long companyId, LocalDate from, LocalDate to) {
    ClaimsExperienceView view = claims.getIfAvailable();
    return view == null ? List.of() : view.movements(companyId, from, to);
  }

  /**
   * Reinsurers' share of the outstanding reserve by claim.
   *
   * @param companyId company
   * @param asOf date
   * @return base amount by claim id, empty without reinsurance module
   */
  public Map<Long, BigDecimal> reinsuranceShare(Long companyId, LocalDate asOf) {
    ClaimReinsuranceView view = reinsurance.getIfAvailable();
    return view == null ? Map.of() : view.reinsuranceShareOfOutstanding(companyId, asOf);
  }
}
