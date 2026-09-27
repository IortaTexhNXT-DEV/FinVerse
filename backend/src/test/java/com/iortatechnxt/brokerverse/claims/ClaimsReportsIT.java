package com.iortatechnxt.brokerverse.claims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.ClaimRepository;
import com.iortatechnxt.brokerverse.claims.domain.ClaimStatus;
import com.iortatechnxt.brokerverse.claims.seed.ClaimsSeedData;
import com.iortatechnxt.brokerverse.claims.service.ClaimLifecycleService;
import com.iortatechnxt.brokerverse.claims.service.ClaimService;
import com.iortatechnxt.brokerverse.claims.service.LpoService;
import com.iortatechnxt.brokerverse.claims.service.RecoveryService;
import com.iortatechnxt.brokerverse.claims.service.ReserveService;
import com.iortatechnxt.brokerverse.claims.service.SettlementService;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.underwriting.domain.PolicyRepository;
import com.iortatechnxt.brokerverse.underwriting.seed.SeedUserContext;
import com.iortatechnxt.brokerverse.underwriting.seed.UnderwritingSeedData;
import com.iortatechnxt.brokerverse.underwriting.service.EndorsementService;
import com.iortatechnxt.brokerverse.underwriting.service.OpenCoverService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyApprovalService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyService;
import com.iortatechnxt.brokerverse.underwriting.service.ProductService;
import com.iortatechnxt.brokerverse.underwriting.service.QuotationService;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;

/** Claims seed portfolio and every claims report with its PDF / XLSX / CSV export. */
@IntegrationTest
class ClaimsReportsIT {

  private static final String[] CODES = {
    "PGIBR002",
    "PGIBR018",
    "PGIBR036",
    "PGIBR012",
    "PGIBR028",
    "PGIBR023",
    "PGIBR082",
    "CLM-REGISTER",
    "CLM-MOVEMENT"
  };

  private static List<Claim> seedClaims;

  @Autowired private ReportService reports;
  @Autowired private TestData data;
  @Autowired private OrganizationService organization;
  @Autowired private PolicyRepository policyRepository;
  @Autowired private ProductService products;
  @Autowired private PolicyService policies;
  @Autowired private PolicyApprovalService approvals;
  @Autowired private EndorsementService endorsements;
  @Autowired private QuotationService quotations;
  @Autowired private OpenCoverService openCovers;
  @Autowired private SeedUserContext users;
  @Autowired private ClaimRepository claimRepository;
  @Autowired private PolicyQueryService policyQuery;
  @Autowired private ClaimService claims;
  @Autowired private ReserveService reserves;
  @Autowired private SettlementService settlements;
  @Autowired private RecoveryService recoveries;
  @Autowired private LpoService lpos;
  @Autowired private ClaimLifecycleService lifecycle;

  private ClaimsSeedData claimsLoader() {
    return new ClaimsSeedData(
        organization,
        claimRepository,
        policyQuery,
        claims,
        reserves,
        settlements,
        recoveries,
        lpos,
        lifecycle,
        users);
  }

  @BeforeEach
  void loadSeedPortfolioOnce() {
    synchronized (ClaimsReportsIT.class) {
      if (seedClaims == null) {
        Long company = data.company().getId();
        new UnderwritingSeedData(
                organization,
                policyRepository,
                products,
                policies,
                approvals,
                endorsements,
                quotations,
                openCovers,
                users)
            .load(company);
        seedClaims = claimsLoader().load(company);
      }
    }
  }

  private Map<String, String> params(String... pairs) {
    Map<String, String> p = new HashMap<>();
    p.put("companyId", data.company().getId().toString());
    p.put("fromDate", "2026-01-01");
    p.put("toDate", "2027-12-31");
    p.put("asOnDate", "2026-09-30");
    p.put(
        "claimNo",
        seedClaims.stream()
            .filter(c -> c.getStatus() == ClaimStatus.CLOSED)
            .findFirst()
            .orElseThrow()
            .getClaimNo());
    for (int i = 0; i < pairs.length; i += 2) {
      p.put(pairs[i], pairs[i + 1]);
    }
    return p;
  }

  private static long details(ReportResult r) {
    return r.rows().stream().filter(row -> row.kind() == RowKind.DETAIL).count();
  }

  @Test
  void seedPortfolioCoversEveryStatusAndIsIdempotent() {
    assertThat(seedClaims).hasSizeBetween(50, 65);
    Set<ClaimStatus> statuses =
        seedClaims.stream().map(Claim::getStatus).collect(Collectors.toSet());
    assertThat(statuses)
        .containsAll(
            EnumSet.of(
                ClaimStatus.OPEN,
                ClaimStatus.PARTIALLY_SETTLED,
                ClaimStatus.CLOSED,
                ClaimStatus.REOPENED,
                ClaimStatus.REJECTED,
                ClaimStatus.WITHDRAWN));
    assertThat(seedClaims).anyMatch(c -> "USD".equals(c.getCurrency()));
    assertThat(seedClaims).anyMatch(c -> c.getPolicy().leadsCoinsurance());
    assertThat(seedClaims).anyMatch(c -> c.getTotals().getRecovered().signum() > 0);
    assertThat(seedClaims.stream().map(c -> c.getPolicy().getBusinessLine()).distinct().count())
        .isGreaterThanOrEqualTo(6);
    Long company = data.company().getId();
    long before = claimRepository.countByCompanyId(company);
    claimsLoader().run(null);
    assertThat(claimRepository.countByCompanyId(company)).isEqualTo(before);
  }

  @Test
  @WithUserDetails("fmanager")
  void everyClaimsReportRunsAndExports() {
    assertThat(reports.catalogue()).extracting("code").contains((Object[]) CODES);
    for (String code : CODES) {
      Map<String, String> p = params();
      ReportResult result = reports.run(code, p);
      assertThat(result.code()).isEqualTo(code);
      assertThat(details(result)).as("rows of %s", code).isPositive();
      for (ExportFormat format : ExportFormat.values()) {
        assertThat(reports.export(code, p, format).content()).isNotEmpty();
      }
    }
  }

  @Test
  @WithUserDetails("fmanager")
  void reportOptionsAndFiltersNarrowTheResult() {
    long all = details(reports.run("PGIBR018", params()));
    long lossOnly = details(reports.run("PGIBR018", params("includeExpense", "false")));
    assertThat(lossOnly).isPositive().isLessThanOrEqualTo(all);
    assertThat(details(reports.run("PGIBR018", params("businessLine", "MOTOR")))).isLessThan(all);
    assertThat(details(reports.run("PGIBR082", params("cover", "OD")))).isPositive();
    assertThat(
            details(
                reports.run(
                    "PGIBR082",
                    params("cover", "TP", "fromDate", "2020-01-01", "toDate", "2020-12-31"))))
        .isZero();
    assertThat(
            details(
                reports.run("PGIBR002", params("fromDate", "2020-01-01", "toDate", "2020-12-31"))))
        .isZero();
    assertThatThrownBy(() -> reports.run("CLM-MOVEMENT", params("claimNo", "CL-NONE")))
        .hasMessageContaining("Unknown claim");
  }
}
