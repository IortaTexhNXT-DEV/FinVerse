package com.iortatechnxt.brokerverse.underwriting;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.underwriting.demo.DemoUserContext;
import com.iortatechnxt.brokerverse.underwriting.demo.UnderwritingDemoData;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import com.iortatechnxt.brokerverse.underwriting.domain.PolicyRepository;
import com.iortatechnxt.brokerverse.underwriting.domain.PolicyStatus;
import com.iortatechnxt.brokerverse.underwriting.domain.QuotationStatus;
import com.iortatechnxt.brokerverse.underwriting.service.EndorsementService;
import com.iortatechnxt.brokerverse.underwriting.service.OpenCoverService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyApprovalService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyService;
import com.iortatechnxt.brokerverse.underwriting.service.ProductService;
import com.iortatechnxt.brokerverse.underwriting.service.QuotationService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;

@IntegrationTest
class UnderwritingReportsIT {

  private static final String[] CODES = {
    "PGIBR042",
    "PGIBR005",
    "PGIBR040",
    "PGIBR003",
    "PGIBR015",
    "PGIBR016",
    "PGIBR043",
    "PGIBR025",
    "PGIBR013",
    "PGIBR027",
    "QTN-SUMMARY",
    "PGIBR084",
    "PGIBR085"
  };

  private static List<Policy> demoPolicies;

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

  private UnderwritingDemoData loader() {
    return new UnderwritingDemoData(
        organization,
        policyRepository,
        products,
        policies,
        approvals,
        endorsements,
        quotations,
        openCovers,
        users);
  }

  @BeforeEach
  void loadDemoPortfolioOnce() {
    synchronized (UnderwritingReportsIT.class) {
      if (demoPolicies == null) {
        demoPolicies = loader().load(data.company().getId());
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
  void demoPortfolioCoversEveryStatus() {
    assertThat(demoPolicies).hasSize(150);
    assertThat(demoPolicies).anyMatch(p -> p.getStatus() == PolicyStatus.PENDING_APPROVAL);
    assertThat(demoPolicies).anyMatch(p -> p.getStatus() == PolicyStatus.DRAFT);
    Long company = data.company().getId();
    for (QuotationStatus s : QuotationStatus.values()) {
      assertThat(quotations.search(company, s, null, null)).as("quotations %s", s).isNotEmpty();
    }
    assertThat(openCovers.list(company)).isNotEmpty();
    long before = policyRepository.countByCompanyId(company);
    loader().run(null);
    assertThat(policyRepository.countByCompanyId(company)).isEqualTo(before);
  }

  @Test
  @WithUserDetails("fmanager")
  void everyUnderwritingReportRunsAndExports() {
    assertThat(reports.catalogue()).extracting("code").contains((Object[]) CODES);
    for (String code : CODES) {
      Map<String, String> p = params("expiryFrom", "2026-01-01", "asOfDate", "2026-09-22");
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
  void registersHonourFiltersAndOptions() {
    long all = details(reports.run("PGIBR005", params()));
    long policiesOnly = details(reports.run("PGIBR005", params("transactions", "POLICIES")));
    long endorsementsOnly =
        details(reports.run("PGIBR005", params("transactions", "ENDORSEMENTS")));
    assertThat(policiesOnly + endorsementsOnly).isEqualTo(all);
    assertThat(endorsementsOnly).isPositive();

    String branch = data.branch("CEB").getId().toString();
    assertThat(details(reports.run("PGIBR015", params("branchId", branch)))).isLessThan(all);
    assertThat(details(reports.run("PGIBR015", params("businessLine", "FIRE")))).isPositive();
    assertThat(details(reports.run("PGIBR016", params("productCode", "MOTOR-PC")))).isPositive();
    assertThat(details(reports.run("PGIBR005", params("customerCode", "C-000201")))).isPositive();
    assertThat(
            details(
                reports.run("PGIBR043", params("brokerCode", "B-0001", "sourceType", "BROKER"))))
        .isPositive();
    assertThat(details(reports.run("PGIBR043", params("sourceType", "DIRECT")))).isPositive();
    assertThat(details(reports.run("PGIBR005", params("basedOn", "ISSUE")))).isPositive();
    assertThat(details(reports.run("PGIBR015", params("basedOn", "PERIOD_FROM")))).isPositive();
    assertThat(details(reports.run("PGIBR003", params("basedOn", "ISSUE")))).isPositive();
    assertThat(
            details(reports.run("PGIBR042", params("pendingDays", "5", "asOfDate", "2026-12-31"))))
        .isPositive();
    assertThat(details(reports.run("PGIBR042", params("pendingDays", "400")))).isZero();
    assertThat(
            details(
                reports.run("PGIBR085", params("zone", "NCR-MAKATI", "asOfDate", "2026-09-22"))))
        .isPositive();
    assertThat(
            details(
                reports.run(
                    "PGIBR013", params("expiryFrom", "2026-01-01", "expiryTo", "2027-12-31"))))
        .isPositive();
    String cover = openCovers.list(data.company().getId()).get(0).getOpenCoverNo();
    assertThat(details(reports.run("PGIBR027", params("openCoverNo", cover)))).isPositive();
    assertThat(details(reports.run("PGIBR027", params("openCoverNo", "NONE")))).isZero();
  }
}
