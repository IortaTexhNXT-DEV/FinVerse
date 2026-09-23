package com.iortatechnxt.finverse.reserves;

import com.iortatechnxt.finverse.insurance.ClaimMovement;
import com.iortatechnxt.finverse.insurance.ClaimReinsuranceView;
import com.iortatechnxt.finverse.insurance.ClaimsExperienceView;
import com.iortatechnxt.finverse.insurance.OutstandingClaim;
import com.iortatechnxt.finverse.underwriting.service.ClaimsFigures;
import com.iortatechnxt.finverse.underwriting.service.PolicyClaimsView;
import com.iortatechnxt.finverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.finverse.underwriting.service.PolicyReinsuranceView;
import com.iortatechnxt.finverse.underwriting.service.PremiumTransaction;
import com.iortatechnxt.finverse.underwriting.service.ReinsuranceFigures;
import com.iortatechnxt.finverse.underwriting.service.TransactionRef;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test implementations of the ports the claims and reinsurance modules provide (insurance shared
 * kernel and underwriting ports), imported only by the tests that need them present. Tests set the
 * data; {@link #reset()} clears it.
 */
@TestConfiguration
public class ReservesTestViews {

  /** Open claims returned by the claims view. */
  public static final List<OutstandingClaim> OUTSTANDING = new ArrayList<>();

  /** Claim movements returned by the claims view. */
  public static final List<ClaimMovement> MOVEMENTS = new ArrayList<>();

  /** Reinsurers' share of outstanding by claim id. */
  public static final Map<Long, BigDecimal> CLAIM_RI = new HashMap<>();

  /** Incurred claims per policy for the takaful surplus. */
  public static final Map<Long, BigDecimal> POLICY_CLAIMS = new HashMap<>();

  private static final BigDecimal TREATY = new BigDecimal("0.30");
  private static final BigDecimal FAC = new BigDecimal("0.10");

  /** Clears the data of every view. */
  public static void reset() {
    OUTSTANDING.clear();
    MOVEMENTS.clear();
    CLAIM_RI.clear();
    POLICY_CLAIMS.clear();
  }

  @Bean
  @Primary
  ClaimsExperienceView testClaimsExperience() {
    return new ClaimsExperienceView() {
      @Override
      public List<OutstandingClaim> outstanding(Long companyId, LocalDate asOf) {
        return List.copyOf(OUTSTANDING);
      }

      @Override
      public List<ClaimMovement> movements(Long companyId, LocalDate from, LocalDate to) {
        return MOVEMENTS.stream()
            .filter(m -> !m.movementDate().isBefore(from) && !m.movementDate().isAfter(to))
            .toList();
      }
    };
  }

  @Bean
  @Primary
  ClaimReinsuranceView testClaimReinsurance() {
    return (companyId, asOf) -> Map.copyOf(CLAIM_RI);
  }

  /** Cedes 30 % of every premium transaction to treaties and 10 % facultatively. */
  @Bean
  @Primary
  PolicyReinsuranceView testPolicyReinsurance(PolicyQueryService policies) {
    return refs -> figures(policies, refs);
  }

  @Bean
  @Primary
  PolicyClaimsView testPolicyClaims() {
    return ids -> {
      Map<Long, ClaimsFigures> out = new HashMap<>();
      ids.stream()
          .filter(POLICY_CLAIMS::containsKey)
          .forEach(
              id ->
                  out.put(
                      id,
                      new ClaimsFigures(
                          1,
                          "CL-TEST",
                          null,
                          BigDecimal.ZERO,
                          POLICY_CLAIMS.get(id),
                          BigDecimal.ZERO,
                          POLICY_CLAIMS.get(id))));
      return out;
    };
  }

  private static Map<TransactionRef, ReinsuranceFigures> figures(
      PolicyQueryService policies, Collection<TransactionRef> refs) {
    Map<TransactionRef, ReinsuranceFigures> out = new HashMap<>();
    refs.stream()
        .map(TransactionRef::policyId)
        .distinct()
        .flatMap(id -> policies.policyTransactions(id).stream())
        .filter(t -> refs.contains(t.ref()))
        .forEach(t -> out.put(t.ref(), split(t)));
    return out;
  }

  private static ReinsuranceFigures split(PremiumTransaction t) {
    BigDecimal net = t.premium().getOurNetPremium();
    BigDecimal treaty = net.multiply(TREATY).setScale(2, RoundingMode.HALF_EVEN);
    BigDecimal fac = net.multiply(FAC).setScale(2, RoundingMode.HALF_EVEN);
    return new ReinsuranceFigures(treaty, fac, net.subtract(treaty).subtract(fac));
  }
}
