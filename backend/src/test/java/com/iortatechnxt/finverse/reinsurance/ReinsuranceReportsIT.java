package com.iortatechnxt.finverse.reinsurance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.reinsurance.demo.ReinsuranceDemoData;
import com.iortatechnxt.finverse.reinsurance.demo.ReinsuranceStatementsDemoData;
import com.iortatechnxt.finverse.reinsurance.domain.FacStatus;
import com.iortatechnxt.finverse.reinsurance.domain.SoaStatus;
import com.iortatechnxt.finverse.reinsurance.service.AllocationRunService;
import com.iortatechnxt.finverse.reinsurance.service.ClaimRecoveryService;
import com.iortatechnxt.finverse.reinsurance.service.FacPlacementService;
import com.iortatechnxt.finverse.reinsurance.service.SoaService;
import com.iortatechnxt.finverse.reinsurance.service.TreatyService;
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
import com.iortatechnxt.finverse.underwriting.service.PolicyService;
import com.iortatechnxt.finverse.underwriting.service.ProductService;
import com.iortatechnxt.finverse.underwriting.service.QuotationService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;

/**
 * Runs the reinsurance demo loader over the underwriting demo portfolio and every reinsurance
 * report on it, exported to PDF, XLSX and CSV.
 */
@IntegrationTest
class ReinsuranceReportsIT {

  private static final String[] CODES = {
    "PGIR0637",
    "PGIR0692",
    "PGIR0693",
    "PGIR0638",
    "PGIR0696",
    "PGIR0639",
    "RI-SOA",
    "RISK-PROFILE",
    "RI-BDX",
    "RI-BAL"
  };

  private static boolean loaded;

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
  @Autowired private TreatyService treaties;
  @Autowired private AllocationRunService allocation;
  @Autowired private FacPlacementService placements;
  @Autowired private ClaimRecoveryService claims;
  @Autowired private SoaService statements;

  private ReinsuranceDemoData loader() {
    return new ReinsuranceDemoData(organization, treaties, allocation, placements, claims, users);
  }

  @BeforeEach
  void loadDemoDataOnce() {
    synchronized (ReinsuranceReportsIT.class) {
      if (!loaded) {
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
            .load(data.company().getId());
        loader().load(data.company().getId());
        new ReinsuranceStatementsDemoData(organization, treaties, statements, users)
            .load(data.company().getId());
        loaded = true;
      }
    }
  }

  private Map<String, String> params(String... pairs) {
    Map<String, String> p = new HashMap<>();
    p.put("companyId", data.company().getId().toString());
    p.put("fromDate", "2026-01-01");
    p.put("toDate", "2026-12-31");
    for (int i = 0; i < pairs.length; i += 2) {
      p.put(pairs[i], pairs[i + 1]);
    }
    return p;
  }

  private static long details(ReportResult r) {
    return r.rows().stream().filter(row -> row.kind() == RowKind.DETAIL).count();
  }

  @Test
  void demoProgrammeIsLoadedOnce() {
    Long company = data.company().getId();
    int before = treaties.list(company).size();
    assertThat(before).isGreaterThanOrEqualTo(5);
    assertThat(placements.list(company, FacStatus.PLACED)).isNotEmpty();
    assertThat(statements.list(company)).anyMatch(s -> s.getStatus() == SoaStatus.SETTLED);
    loader().run(null);
    assertThat(treaties.list(company)).hasSize(before);
  }

  @Test
  @WithUserDetails("fmanager")
  void everyReinsuranceReportRunsAndExports() {
    assertThat(reports.catalogue()).extracting("code").contains((Object[]) CODES);
    Map<String, String> p =
        params(
            "treatyYear", "2026",
            "treatyCode", "FIRE-QS-26",
            "quarter", "1",
            "statementDate", "2026-04-15",
            "asOnDate", "2026-09-22",
            "asOfDate", "2026-09-22",
            "uwYearFrom", "2026",
            "uwYearTo", "2026");
    for (String code : CODES) {
      ReportResult result = reports.run(code, p);
      assertThat(result.code()).isEqualTo(code);
      for (ExportFormat format : ExportFormat.values()) {
        assertThat(reports.export(code, p, format).content()).isNotEmpty();
      }
    }
    assertThat(details(reports.run("PGIR0637", p))).isPositive();
    assertThat(details(reports.run("PGIR0692", p))).isPositive();
    assertThat(details(reports.run("PGIR0693", p))).isPositive();
    assertThat(details(reports.run("RI-SOA", p))).isEqualTo(3L * 11);
    assertThat(details(reports.run("RI-BAL", p))).isPositive();
    assertThat(details(reports.run("RI-BDX", p))).isPositive();
  }

  @Test
  @WithUserDetails("fmanager")
  void filtersNarrowTheRegisters() {
    long all = details(reports.run("PGIR0637", params()));
    long fire = details(reports.run("PGIR0637", params("businessLine", "FIRE")));
    long facOnly = details(reports.run("PGIR0637", params("treatyType", "FAC")));
    long provisional = details(reports.run("PGIR0637", params("allocationStatus", "PROVISIONAL")));
    long allocated = details(reports.run("PGIR0637", params("allocationStatus", "ALLOCATED")));
    assertThat(fire).isPositive().isLessThan(all);
    assertThat(facOnly).isPositive().isLessThan(all);
    assertThat(provisional + allocated).isEqualTo(all);
    assertThat(
            details(
                reports.run("RI-BDX", params("treatyCode", "FIRE-SP-26", "bordereau", "CLAIMS"))))
        .isNotNegative();
    assertThat(
            details(
                reports.run(
                    "RI-SOA",
                    params(
                        "treatyYear", "2026",
                        "treatyCode", "MOT-XL-26",
                        "quarter", "2",
                        "statementDate", "2026-07-15",
                        "reinsurerCode", "R-0001"))))
        .isEqualTo(11L);
    assertThatThrownBy(
            () ->
                reports.run(
                    "RI-SOA",
                    params("treatyYear", "2025", "treatyCode", "FIRE-QS-26", "quarter", "1")))
        .isInstanceOf(BusinessRuleException.class);
  }
}
