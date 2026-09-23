package com.iortatechnxt.finverse.claims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.claims.demo.ClaimsDemoData;
import com.iortatechnxt.finverse.claims.domain.Claim;
import com.iortatechnxt.finverse.claims.domain.ClaimRepository;
import com.iortatechnxt.finverse.claims.domain.ClaimStatus;
import com.iortatechnxt.finverse.claims.service.ClaimLifecycleService;
import com.iortatechnxt.finverse.claims.service.ClaimService;
import com.iortatechnxt.finverse.claims.service.LpoService;
import com.iortatechnxt.finverse.claims.service.RecoveryService;
import com.iortatechnxt.finverse.claims.service.ReserveService;
import com.iortatechnxt.finverse.claims.service.SettlementService;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.ReportService;
import com.iortatechnxt.finverse.report.core.RowKind;
import com.iortatechnxt.finverse.report.render.ExportFormat;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestData;
import com.iortatechnxt.finverse.underwriting.demo.DemoUserContext;
import com.iortatechnxt.finverse.underwriting.demo.UnderwritingDemoData;
import com.iortatechnxt.finverse.underwriting.domain.PolicyRepository;
import com.iortatechnxt.finverse.underwriting.service.EndorsementService;
import com.iortatechnxt.finverse.underwriting.service.OpenCoverService;
import com.iortatechnxt.finverse.underwriting.service.PolicyApprovalService;
import com.iortatechnxt.finverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.finverse.underwriting.service.PolicyService;
import com.iortatechnxt.finverse.underwriting.service.ProductService;
import com.iortatechnxt.finverse.underwriting.service.QuotationService;
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

/** Claims demo portfolio and every claims report with its PDF / XLSX / CSV export. */
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

  private static List<Claim> demoClaims;

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
  @Autowired private DemoUserContext users;
  @Autowired private ClaimRepository claimRepository;
  @Autowired private PolicyQueryService policyQuery;
  @Autowired private ClaimService claims;
  @Autowired private ReserveService reserves;
  @Autowired private SettlementService settlements;
  @Autowired private RecoveryService recoveries;
  @Autowired private LpoService lpos;
  @Autowired private ClaimLifecycleService lifecycle;

  private ClaimsDemoData claimsLoader() {
    return new ClaimsDemoData(
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
  void loadDemoPortfolioOnce() {
    synchronized (ClaimsReportsIT.class) {
      if (demoClaims == null) {
        Long company = data.company().getId();
        new UnderwritingDemoData(
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
        demoClaims = claimsLoader().load(company);
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
        demoClaims.stream()
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
  void demoPortfolioCoversEveryStatusAndIsIdempotent() {
    assertThat(demoClaims).hasSizeBetween(50, 65);
    Set<ClaimStatus> statuses =
        demoClaims.stream().map(Claim::getStatus).collect(Collectors.toSet());
    assertThat(statuses)
        .containsAll(
            EnumSet.of(
                ClaimStatus.OPEN,
                ClaimStatus.PARTIALLY_SETTLED,
                ClaimStatus.CLOSED,
                ClaimStatus.REOPENED,
                ClaimStatus.REJECTED,
                ClaimStatus.WITHDRAWN));
    assertThat(demoClaims).anyMatch(c -> "USD".equals(c.getCurrency()));
    assertThat(demoClaims).anyMatch(c -> c.getPolicy().leadsCoinsurance());
    assertThat(demoClaims).anyMatch(c -> c.getTotals().getRecovered().signum() > 0);
    assertThat(demoClaims.stream().map(c -> c.getPolicy().getBusinessLine()).distinct().count())
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
