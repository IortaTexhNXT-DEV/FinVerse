package com.iortatechnxt.brokerverse.claims.seed;

import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.ClaimRepository;
import com.iortatechnxt.brokerverse.claims.seed.SeedClaimPlan.PlannedClaim;
import com.iortatechnxt.brokerverse.claims.service.ClaimLifecycleService;
import com.iortatechnxt.brokerverse.claims.service.ClaimService;
import com.iortatechnxt.brokerverse.claims.service.LpoService;
import com.iortatechnxt.brokerverse.claims.service.RecoveryService;
import com.iortatechnxt.brokerverse.claims.service.ReserveService;
import com.iortatechnxt.brokerverse.claims.service.SettlementService;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.underwriting.seed.SeedUserContext;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicySnapshot;
import com.iortatechnxt.brokerverse.underwriting.service.PremiumTransaction;
import com.iortatechnxt.brokerverse.underwriting.service.RiskSnapshot;
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
 * SEED PROFILE ONLY: creates about 60 claims of the seed company (FVI) at start-up, against the
 * underwriting seed policies (runner order 10, this one 20), through the claims services so that
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
@Profile("seed")
@Order(20)
public class ClaimsSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(ClaimsSeedData.class);
  private static final String SEED_COMPANY = "FVI";
  private static final LocalDate YEAR_START = LocalDate.of(2026, 1, 1);

  private final OrganizationService organization;
  private final ClaimRepository claimRepository;
  private final PolicyQueryService policies;
  private final SeedClaimScenarios scenarios;

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
   * @param users SIT/UAT user context
   */
  public ClaimsSeedData(
      OrganizationService organization,
      ClaimRepository claimRepository,
      PolicyQueryService policies,
      ClaimService claims,
      ReserveService reserves,
      SettlementService settlements,
      RecoveryService recoveries,
      LpoService lpos,
      ClaimLifecycleService lifecycle,
      SeedUserContext users) {
    this.organization = organization;
    this.claimRepository = claimRepository;
    this.policies = policies;
    this.scenarios =
        new SeedClaimScenarios(users, claims, reserves, settlements, recoveries, lpos, lifecycle);
  }

  @Override
  public void run(ApplicationArguments args) {
    organization.listCompanies().stream()
        .filter(c -> SEED_COMPANY.equals(c.getCode()))
        .findFirst()
        .map(Company::getId)
        .ifPresent(this::loadIfEmpty);
  }

  private void loadIfEmpty(Long companyId) {
    if (claimRepository.countByCompanyId(companyId) > 0) {
      LOG.info("Claims seed data already present, skipped");
    } else {
      load(companyId);
    }
  }

  /**
   * Creates the seed claims (no idempotency check).
   *
   * @param companyId seed company
   * @return claims created
   */
  public List<Claim> load(Long companyId) {
    List<PolicySnapshot> candidates =
        policies.approvedTransactions(companyId, YEAR_START, SeedClaimScenarios.LAST_DATE).stream()
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
    for (PlannedClaim pc : SeedClaimPlan.plan(candidates, sumsInsured)) {
      created.add(scenarios.run(companyId, pc));
    }
    LOG.info("Claims seed data created: {} claims", created.size());
    return created;
  }

  private BigDecimal sumInsured(Long policyId) {
    return policies.risks(policyId).stream()
        .findFirst()
        .map(RiskSnapshot::sumInsured)
        .orElse(Money.zero());
  }
}
