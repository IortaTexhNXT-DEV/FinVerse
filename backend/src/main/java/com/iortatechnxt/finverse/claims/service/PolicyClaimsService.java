package com.iortatechnxt.finverse.claims.service;

import com.iortatechnxt.finverse.claims.domain.Claim;
import com.iortatechnxt.finverse.claims.domain.ClaimRepository;
import com.iortatechnxt.finverse.claims.domain.ClaimStatus;
import com.iortatechnxt.finverse.claims.domain.OurShare;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.underwriting.service.ClaimsFigures;
import com.iortatechnxt.finverse.underwriting.service.PolicyClaimsView;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the underwriting port {@link PolicyClaimsView}: claims experience per policy,
 * company share, in the policy currency (a claim's currency is always its policy's currency).
 *
 * <ul>
 *   <li>claim count: claims registered, except withdrawn ones;
 *   <li>latest claim: the one with the latest date of loss;
 *   <li>reserve / outstanding: payment estimate and estimate − paid of claims still being handled;
 *   <li>paid: settled amounts net of recoveries; net claims = paid + outstanding.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class PolicyClaimsService implements PolicyClaimsView {

  private static final Comparator<Claim> LATEST =
      Comparator.comparing((Claim c) -> c.getLoss().getLossDate()).thenComparing(Claim::getId);

  private final ClaimRepository claims;

  /**
   * Creates the view.
   *
   * @param claims claim repository
   */
  public PolicyClaimsService(ClaimRepository claims) {
    this.claims = claims;
  }

  @Override
  public Map<Long, ClaimsFigures> figures(Collection<Long> policyIds) {
    if (policyIds.isEmpty()) {
      return Map.of();
    }
    return claims.findByPolicyIds(policyIds).stream()
        .filter(c -> c.getStatus() != ClaimStatus.WITHDRAWN)
        .collect(Collectors.groupingBy(c -> c.getPolicy().getPolicyId()))
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> figures(e.getValue())));
  }

  private static ClaimsFigures figures(List<Claim> claims) {
    Claim latest = claims.stream().max(LATEST).orElseThrow();
    BigDecimal reserve = Money.zero();
    BigDecimal paid = Money.zero();
    BigDecimal outstanding = Money.zero();
    for (Claim c : claims) {
      OurShare our = c.ourShare();
      paid = paid.add(our.paid()).subtract(our.recovered());
      if (c.getStatus().isActive()) {
        reserve = reserve.add(our.estimate());
        outstanding = outstanding.add(our.outstanding());
      }
    }
    return new ClaimsFigures(
        claims.size(),
        latest.getClaimNo(),
        latest.getLoss().getLossDate(),
        reserve,
        paid,
        outstanding,
        paid.add(outstanding));
  }
}
