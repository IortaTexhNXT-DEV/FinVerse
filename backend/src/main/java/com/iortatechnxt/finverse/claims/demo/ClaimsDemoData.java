package com.iortatechnxt.finverse.claims.demo;

import com.iortatechnxt.finverse.claims.demo.DemoClaimPlan.PlannedClaim;
import com.iortatechnxt.finverse.claims.domain.Claim;
import com.iortatechnxt.finverse.claims.domain.ClaimRepository;
import com.iortatechnxt.finverse.claims.service.ClaimLifecycleService;
import com.iortatechnxt.finverse.claims.service.ClaimService;
import com.iortatechnxt.finverse.claims.service.LpoService;
import com.iortatechnxt.finverse.claims.service.RecoveryService;
import com.iortatechnxt.finverse.claims.service.ReserveService;
import com.iortatechnxt.finverse.claims.service.SettlementService;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.organization.domain.Company;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.underwriting.demo.DemoUserContext;
import com.iortatechnxt.finverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.finverse.underwriting.service.PolicySnapshot;
import com.iortatechnxt.finverse.underwriting.service.PremiumTransaction;
import com.iortatechnxt.finverse.underwriting.service.RiskSnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * DEMO PROFILE ONLY: creates about 60 claims of the demo company (FVI) at start-up, against the
 * underwriting demo policies (runner order 10, this one 20), through the claims services so that
 * reserves, settlements, recoveries, journals, open items and the accounting event register are
 * real. Losses from February to July 2026 across the lines of business, in every status: open,
 * partially settled, closed, repudiated, withdrawn and reopened, with reserve increases and
 * decreases, LPOs for motor repairs, a USD marine claim, coinsured claims and a salvage recovery.
 * Settlements stay unpaid, so the payables module shows them as claim payables (CREDIT open items
 * "CLAIM_SETTLEMENT") ready for payment vouchers.
 *
 * <p>Idempotent: nothing is created when the company already has claims.
 */
@Component
@Profile("demo")
@Order(20)
public class ClaimsDemoData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(ClaimsDemoData.class);
  private static final String DEMO_COMPANY = "FVI";
  private static final LocalDate YEAR_START = LocalDate.of(2026, 1, 1);

  private final OrganizationService organization;
  private final ClaimRepository claimRepository;
  private final PolicyQueryService policies;
  private final DemoClaimScenarios scenarios;

  /**
   * Creates the loader.
   *
   * @param organization companies
   * @param claimRepository claims (idempotency check)
   * @param policies underwriting read API
   * @param claims claim registration
   * @param reserves reserve changes
   * @param settlements settlements
   * @param recoveries recoveries
   * @param lpos local purchase orders
   * @param lifecycle claim decisions
   * @param users demo user context
   */
  public ClaimsDemoData(
      OrganizationService organization,
      ClaimRepository claimRepository,
      PolicyQueryService policies,
      ClaimService claims,
      ReserveService reserves,
      SettlementService settlements,
      RecoveryService recoveries,
      LpoService lpos,
      ClaimLifecycleService lifecycle,
      DemoUserContext users) {
    this.organization = organization;
    this.claimRepository = claimRepository;
    this.policies = policies;
    this.scenarios =
        new DemoClaimScenarios(users, claims, reserves, settlements, recoveries, lpos, lifecycle);
  }

  @Override
  public void run(ApplicationArguments args) {
    organization.listCompanies().stream()
        .filter(c -> DEMO_COMPANY.equals(c.getCode()))
        .findFirst()
        .map(Company::getId)
        .ifPresent(this::loadIfEmpty);
  }

  private void loadIfEmpty(Long companyId) {
    if (claimRepository.countByCompanyId(companyId) > 0) {
      LOG.info("Claims demo data already present, skipped");
    } else {
      load(companyId);
    }
  }

  /**
   * Creates the demo claims (no idempotency check).
   *
   * @param companyId demo company
   * @return claims created
   */
  public List<Claim> load(Long companyId) {
    List<PolicySnapshot> candidates =
        policies.approvedTransactions(companyId, YEAR_START, DemoClaimScenarios.LAST_DATE).stream()
            .filter(t -> PremiumTransaction.NEW.equals(t.kind()))
            .map(PremiumTransaction::policy)
            .sorted(
                Comparator.comparing(PolicySnapshot::issueDate)
                    .thenComparing(PolicySnapshot::policyNo))
            .toList();
    Map<Long, BigDecimal> sumsInsured =
        candidates.stream()
            .collect(Collectors.toMap(PolicySnapshot::id, p -> sumInsured(p.id()), (a, b) -> a));
    List<Claim> created = new ArrayList<>();
    for (PlannedClaim pc : DemoClaimPlan.plan(candidates, sumsInsured)) {
      created.add(scenarios.run(companyId, pc));
    }
    LOG.info("Claims demo data created: {} claims", created.size());
    return created;
  }

  private BigDecimal sumInsured(Long policyId) {
    return policies.risks(policyId).stream()
        .findFirst()
        .map(RiskSnapshot::sumInsured)
        .orElse(Money.zero());
  }
}
