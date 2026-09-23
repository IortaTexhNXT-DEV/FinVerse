package com.iortatechnxt.finverse.underwriting.service;

import java.util.Collection;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Optional access to the ports implemented by other modules, with safe defaults when the
 * implementing module is not deployed (no cession, no claims).
 */
@Component
public class UnderwritingPorts {

  private final ObjectProvider<PolicyReinsuranceView> reinsurance;
  private final ObjectProvider<PolicyClaimsView> claims;

  /**
   * Creates the accessor.
   *
   * @param reinsurance reinsurance view, if any module provides one
   * @param claims claims view, if any module provides one
   */
  public UnderwritingPorts(
      ObjectProvider<PolicyReinsuranceView> reinsurance, ObjectProvider<PolicyClaimsView> claims) {
    this.reinsurance = reinsurance;
    this.claims = claims;
  }

  /**
   * Reinsurance figures of transactions.
   *
   * @param refs transactions
   * @return figures (empty when no reinsurance module is deployed)
   */
  public Map<TransactionRef, ReinsuranceFigures> reinsurance(Collection<TransactionRef> refs) {
    PolicyReinsuranceView view = reinsurance.getIfAvailable();
    return view == null || refs.isEmpty() ? Map.of() : view.figures(refs);
  }

  /**
   * Claims figures of policies.
   *
   * @param policyIds policies
   * @return figures (empty when no claims module is deployed)
   */
  public Map<Long, ClaimsFigures> claims(Collection<Long> policyIds) {
    PolicyClaimsView view = claims.getIfAvailable();
    return view == null || policyIds.isEmpty() ? Map.of() : view.figures(policyIds);
  }
}
