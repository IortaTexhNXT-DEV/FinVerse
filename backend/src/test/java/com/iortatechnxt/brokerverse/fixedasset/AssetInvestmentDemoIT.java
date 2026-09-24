package com.iortatechnxt.brokerverse.fixedasset;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.fixedasset.demo.FixedAssetDemoData;
import com.iortatechnxt.brokerverse.fixedasset.domain.AssetCategoryRepository;
import com.iortatechnxt.brokerverse.fixedasset.domain.AssetStatus;
import com.iortatechnxt.brokerverse.fixedasset.domain.DepreciationRunRepository;
import com.iortatechnxt.brokerverse.fixedasset.domain.FixedAssetRepository;
import com.iortatechnxt.brokerverse.fixedasset.service.AssetLifecycleService;
import com.iortatechnxt.brokerverse.fixedasset.service.DepreciationService;
import com.iortatechnxt.brokerverse.fixedasset.service.FixedAssetService;
import com.iortatechnxt.brokerverse.investment.demo.InvestmentDemoData;
import com.iortatechnxt.brokerverse.investment.domain.HoldingStatus;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentHoldingRepository;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentPortfolioRepository;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentRunRepository;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentTransactionRepository;
import com.iortatechnxt.brokerverse.investment.service.HoldingEventService;
import com.iortatechnxt.brokerverse.investment.service.InvestmentRunService;
import com.iortatechnxt.brokerverse.investment.service.InvestmentService;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import jakarta.persistence.EntityManager;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs both demo data runners (normally active only with the demo profile) inside a rolled back
 * transaction, checks the data set and exercises every fixed asset and investment report on it.
 */
@IntegrationTest
@Transactional
class AssetInvestmentDemoIT {

  private static final String[] REPORTS = {
    "FIN-FA-REG",
    "FIN-FA-DEPR",
    "FIN-FA-MOVE",
    "FIN-FA-NBV",
    "FIN-INV-PORT",
    "FIN-INV-ACCR",
    "FIN-INV-MAT",
    "FIN-INV-RGL",
    "FIN-INV-SECDEP"
  };

  @Autowired private CompanyRepository companies;
  @Autowired private BranchRepository branches;
  @Autowired private AssetCategoryRepository categories;
  @Autowired private FixedAssetRepository assets;
  @Autowired private DepreciationRunRepository depreciationRuns;
  @Autowired private FixedAssetService assetService;
  @Autowired private AssetLifecycleService lifecycle;
  @Autowired private DepreciationService depreciation;
  @Autowired private InvestmentPortfolioRepository portfolios;
  @Autowired private InvestmentHoldingRepository holdings;
  @Autowired private InvestmentTransactionRepository transactions;
  @Autowired private InvestmentRunRepository investmentRuns;
  @Autowired private InvestmentService investmentService;
  @Autowired private HoldingEventService holdingEvents;
  @Autowired private InvestmentRunService runService;
  @Autowired private ReportService reports;
  @Autowired private EntityManager entityManager;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  @Test
  void demoDataIsBuiltIdempotentlyAndEveryReportRunsAndExports() {
    FixedAssetDemoData assetDemo =
        new FixedAssetDemoData(
            companies, branches, categories, assets, assetService, lifecycle, depreciation);
    InvestmentDemoData investmentDemo =
        new InvestmentDemoData(
            companies,
            branches,
            portfolios,
            holdings,
            transactions,
            investmentService,
            holdingEvents,
            runService);

    assetDemo.run(null);
    investmentDemo.run(null);
    long journalsAfterFirstRun = transactions.count();
    assetDemo.run(null);
    investmentDemo.run(null);
    entityManager.flush();

    Long company = data.company().getId();
    assertThat(transactions.count()).isEqualTo(journalsAfterFirstRun);
    assertThat(assets.findAll()).hasSize(25);
    assertThat(assets.findAll()).filteredOn(a -> a.getStatus() == AssetStatus.DISPOSED).hasSize(1);
    assertThat(depreciationRuns.findByCompanyIdOrderByPeriodDesc(company)).hasSize(9);
    assertThat(holdings.findAll()).hasSize(12);
    assertThat(holdings.findAll())
        .filteredOn(h -> h.getStatus() == HoldingStatus.MATURED)
        .hasSize(2);
    assertThat(holdings.findAll()).filteredOn(h -> h.getStatus() == HoldingStatus.SOLD).hasSize(1);
    assertThat(investmentRuns.findByCompanyIdOrderByPeriodDescRunTypeAsc(company)).hasSize(18);

    Map<String, String> params =
        Map.of(
            "companyId", company.toString(),
            "asOfDate", "2026-09-30",
            "fromDate", "2026-01-01",
            "toDate", "2026-09-30");
    for (String code : REPORTS) {
      ReportResult result = as.run("fmanager", () -> reports.run(code, params));
      assertThat(result.rows()).as(code).anyMatch(r -> r.kind() == RowKind.DETAIL);
      for (ExportFormat format : ExportFormat.values()) {
        var file = as.run("fmanager", () -> reports.export(code, params, format));
        assertThat(file.content()).as(code + " " + format).isNotEmpty();
      }
    }
    ReportResult byClass =
        as.run(
            "fmanager",
            () ->
                reports.run(
                    "FIN-INV-PORT",
                    Map.of(
                        "companyId",
                        company.toString(),
                        "asOfDate",
                        "2026-09-30",
                        "groupBy",
                        "CLASSIFICATION",
                        "branchId",
                        data.branch("HO").getId().toString())));
    assertThat(byClass.rows()).anyMatch(r -> r.kind() == RowKind.GROUP_HEADER);
  }
}
