package com.iortatechnxt.brokerverse.approval;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.alert.service.PendingApprovalAgeingCheck;
import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.budget.api.dto.BudgetLineRequest;
import com.iortatechnxt.brokerverse.budget.api.dto.CreateBudgetRequest;
import com.iortatechnxt.brokerverse.budget.domain.Budget;
import com.iortatechnxt.brokerverse.budget.domain.BudgetVersionType;
import com.iortatechnxt.brokerverse.budget.service.BudgetLineService;
import com.iortatechnxt.brokerverse.budget.service.BudgetService;
import com.iortatechnxt.brokerverse.fixedasset.api.dto.AssetCategoryRequest;
import com.iortatechnxt.brokerverse.fixedasset.api.dto.FixedAssetRequest;
import com.iortatechnxt.brokerverse.fixedasset.domain.AssetCategory;
import com.iortatechnxt.brokerverse.fixedasset.domain.AssetCategoryRepository;
import com.iortatechnxt.brokerverse.fixedasset.domain.DepreciationMethod;
import com.iortatechnxt.brokerverse.fixedasset.domain.FixedAsset;
import com.iortatechnxt.brokerverse.fixedasset.service.AssetCategoryService;
import com.iortatechnxt.brokerverse.fixedasset.service.FixedAssetService;
import com.iortatechnxt.brokerverse.investment.api.dto.HoldingRequest;
import com.iortatechnxt.brokerverse.investment.api.dto.PortfolioRequest;
import com.iortatechnxt.brokerverse.investment.domain.Classification;
import com.iortatechnxt.brokerverse.investment.domain.CouponFrequency;
import com.iortatechnxt.brokerverse.investment.domain.DayCountConvention;
import com.iortatechnxt.brokerverse.investment.domain.InstrumentType;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentHolding;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentPortfolio;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentPortfolioRepository;
import com.iortatechnxt.brokerverse.investment.service.InvestmentService;
import com.iortatechnxt.brokerverse.investment.service.PortfolioService;
import com.iortatechnxt.brokerverse.receivables.ReceivablesFixtures;
import com.iortatechnxt.brokerverse.receivables.domain.AllocationMethod;
import com.iortatechnxt.brokerverse.receivables.domain.PayerType;
import com.iortatechnxt.brokerverse.receivables.domain.Receipt;
import com.iortatechnxt.brokerverse.receivables.domain.ReceiptMode;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestCompanies;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Receivables, budget, fixed asset and investment items in the universal approval inbox, and the
 * pending-approval ageing alert over them. Every test rolls back.
 */
@IntegrationTest
@Transactional
class BusinessApprovalSourcesIT {

  @Autowired private ApprovalInboxService inbox;
  @Autowired private ReceivablesFixtures receivables;
  @Autowired private BudgetService budgets;
  @Autowired private BudgetLineService budgetLines;
  @Autowired private FixedAssetService register;
  @Autowired private AssetCategoryService categoryService;
  @Autowired private AssetCategoryRepository categories;
  @Autowired private InvestmentService holdings;
  @Autowired private PortfolioService portfolioService;
  @Autowired private InvestmentPortfolioRepository portfolios;
  @Autowired private TestCompanies companies;
  @Autowired private TestData data;
  @Autowired private AlertService alerts;
  @Autowired private AsUser as;

  private Long company() {
    return data.company().getId();
  }

  private Map<String, PendingApproval> inboxOf(String user) {
    return as.run(user, () -> inbox.inbox(null)).stream()
        .collect(Collectors.toMap(PendingApproval::reference, Function.identity(), (a, b) -> a));
  }

  @Test
  void pendingReceiptReachesTheCheckerButNotItsMaker() {
    Receipt receipt =
        receivables.create(
            receivables
                .request(
                    PayerType.OTHER,
                    null,
                    LocalDate.of(2026, 5, 15),
                    ReceiptMode.CASH,
                    "750.25",
                    "1112")
                .with(AllocationMethod.MANUAL, List.of()));

    PendingApproval item = inboxOf("checker").get(receipt.getReceiptNo());
    assertThat(item.module()).isEqualTo("RECEIVABLES");
    assertThat(item.link()).isEqualTo("/receivables/approvals/" + receipt.getId());
    assertThat(item.amount()).isEqualByComparingTo("750.25");
    assertThat(item.currency()).isEqualTo("PHP");
    assertThat(inboxOf("accountant")).doesNotContainKey(receipt.getReceiptNo());
  }

  @Test
  void submittedBudgetReachesTheApproverButNotItsSubmitter() {
    Long companyId = companies.create("TAPR", "PHP").getId();
    companies.openYear(companyId, 2026);
    Budget draft =
        as.run(
            "accountant",
            () ->
                budgets.create(
                    new CreateBudgetRequest(
                        companyId, 2026, BudgetVersionType.ORIGINAL, "Inbox budget", null)));
    as.run(
        "accountant",
        () ->
            budgetLines.saveLines(
                draft.getId(),
                List.of(
                    new BudgetLineRequest(
                        "5601", "FIN", Collections.nCopies(12, new BigDecimal("1000"))))));
    as.run("accountant", () -> budgets.submit(draft.getId()));

    String link = "/planning/budgets/" + draft.getId();
    PendingApproval item =
        as.run("fmanager", () -> inbox.inbox(companyId)).stream()
            .filter(i -> link.equals(i.link()))
            .findFirst()
            .orElseThrow();
    assertThat(item.module()).isEqualTo("BUDGET");
    assertThat(item.reference()).isEqualTo("FY2026 v1");
    assertThat(item.amount()).isEqualByComparingTo("12000");
    assertThat(item.submittedBy()).isEqualTo("accountant");
    assertThat(as.run("accountant", () -> inbox.inbox(companyId))).isEmpty();
    assertThat(as.run("checker", () -> inbox.inbox(companyId)))
        .noneMatch(i -> link.equals(i.link()));
  }

  @Test
  void assetsAndCategoriesAwaitingAuthorizationReachTheChecker() {
    FixedAsset asset = as.run("accountant", () -> register.create(assetRequest()));
    AssetCategory category =
        as.run(
            "accountant",
            () ->
                categoryService.create(
                    new AssetCategoryRequest(
                        company(),
                        "T-INBOX",
                        "Inbox category",
                        "1701",
                        "1709",
                        "5611",
                        DepreciationMethod.STRAIGHT_LINE,
                        24,
                        BigDecimal.ONE)));

    Map<String, PendingApproval> checker = inboxOf("checker");
    PendingApproval item = checker.get(asset.getTagNo());
    assertThat(item.module()).isEqualTo("FIXED_ASSETS");
    assertThat(item.link()).isEqualTo("/assets/register");
    assertThat(item.amount()).isEqualByComparingTo("45000");
    assertThat(item.currency()).isEqualTo("PHP");
    assertThat(checker.get(category.getCode()).link()).isEqualTo("/assets/categories");
    assertThat(inboxOf("accountant")).doesNotContainKeys(asset.getTagNo(), category.getCode());
  }

  @Test
  void holdingsAndPortfoliosAwaitingApprovalReachTheChecker() {
    InvestmentHolding holding = as.run("accountant", () -> holdings.create(holdingRequest()));
    InvestmentPortfolio portfolio =
        as.run(
            "accountant",
            () ->
                portfolioService.create(
                    new PortfolioRequest(
                        company(),
                        "T-INBOX",
                        "Inbox portfolio",
                        Classification.FVPL,
                        "1501",
                        "1504",
                        "4501",
                        "4503",
                        "4504")));

    Map<String, PendingApproval> checker = inboxOf("checker");
    PendingApproval item = checker.get(holding.getHoldingNo());
    assertThat(item.module()).isEqualTo("INVESTMENTS");
    assertThat(item.link()).isEqualTo("/investments/holdings");
    assertThat(item.amount()).isEqualByComparingTo("990000");
    assertThat(checker.get(portfolio.getCode()).link()).isEqualTo("/investments/portfolios");
    assertThat(inboxOf("accountant"))
        .doesNotContainKeys(holding.getHoldingNo(), portfolio.getCode());
  }

  @Test
  void ageingAlertCoversTheItemsOfEveryModule() {
    FixedAsset asset = as.run("accountant", () -> register.create(assetRequest()));
    PendingApprovalAgeingCheck now = new PendingApprovalAgeingCheck(inbox, alerts, clockAt(0));
    PendingApprovalAgeingCheck weekLater =
        new PendingApprovalAgeingCheck(inbox, alerts, clockAt(7));

    assertThat(now.evaluate(LocalDate.of(2026, 9, 30)))
        .noneMatch(s -> asset.getTagNo().equals(s.facts().entityId()));
    assertThat(weekLater.evaluate(LocalDate.of(2026, 9, 30)))
        .anySatisfy(
            s -> {
              assertThat(s.code()).isEqualTo(PendingApprovalAgeingCheck.CODE);
              assertThat(s.facts().dedupKey())
                  .isEqualTo(
                      PendingApprovalAgeingCheck.CODE
                          + ":FIXED_ASSETS:Asset capitalization:"
                          + asset.getTagNo());
              assertThat(s.facts().amount()).isEqualByComparingTo("45000");
            });
  }

  private static Clock clockAt(int daysLater) {
    return Clock.fixed(Instant.now().plus(Duration.ofDays(daysLater)), ZoneOffset.UTC);
  }

  private FixedAssetRequest assetRequest() {
    AssetCategory offeq = categories.findByCompanyIdAndCode(company(), "OFFEQ").orElseThrow();
    return new FixedAssetRequest(
        company(),
        data.branch("HO").getId(),
        offeq.getId(),
        "T-INBOX-1",
        "Inbox test laptop",
        "FIN",
        "S-0001",
        LocalDate.of(2026, 8, 5),
        new BigDecimal("45000"),
        null,
        null,
        "Head office",
        "Finance",
        "2501",
        false,
        null,
        null,
        null);
  }

  private HoldingRequest holdingRequest() {
    return new HoldingRequest(
        company(),
        data.branch("HO").getId(),
        portfolios.findByCompanyIdAndCode(company(), "AC-GOVT").orElseThrow().getId(),
        InstrumentType.GOVERNMENT_BOND,
        "T-INBOX",
        "Inbox test bond",
        "IS-BTR",
        "RoSS",
        "PHP",
        new BigDecimal("1000000"),
        new BigDecimal("990000"),
        null,
        LocalDate.of(2026, 3, 15),
        LocalDate.of(2026, 3, 15),
        LocalDate.of(2031, 3, 15),
        new BigDecimal("6"),
        CouponFrequency.SEMI_ANNUAL,
        DayCountConvention.THIRTY_360,
        null,
        false,
        "1111",
        false,
        null);
  }
}
