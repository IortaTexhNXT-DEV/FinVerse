package com.iortatechnxt.finverse.underwriting.service;

import java.util.Collection;
import java.util.Map;

/**
 * PORT implemented by the claims module: claims experience per policy, used by underwriting reports
 * (renewal list with claim ratio, marine certificate report).
 *
 * <p>Underwriting does not depend on claims: it injects this interface optionally. When no
 * implementation is deployed, or a policy has no entry in the returned map, reports show no claims
 * (see {@link ClaimsFigures#none()}).
 *
 * <p>Implementation contract: read-only, bulk query, company share amounts in the policy currency.
 */
public interface PolicyClaimsView {

  /**
   * Claims figures of policies.
   *
   * @param policyIds policy ids
   * @return figures by policy id; policies without claims may be omitted
   */
  Map<Long, ClaimsFigures> figures(Collection<Long> policyIds);
}
