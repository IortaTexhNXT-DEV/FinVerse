package com.iortatechnxt.brokerverse.investment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.investment.api.dto.CouponReceiptRequest;
import com.iortatechnxt.brokerverse.investment.api.dto.FairValueRequest;
import com.iortatechnxt.brokerverse.investment.api.dto.HoldingRequest;
import com.iortatechnxt.brokerverse.investment.api.dto.PortfolioRequest;
import com.iortatechnxt.brokerverse.investment.api.dto.RedemptionRequest;
import com.iortatechnxt.brokerverse.investment.domain.AmortizationMethod;
import com.iortatechnxt.brokerverse.investment.domain.Classification;
import com.iortatechnxt.brokerverse.investment.domain.CouponFrequency;
import com.iortatechnxt.brokerverse.investment.domain.DayCountConvention;
import com.iortatechnxt.brokerverse.investment.domain.HoldingStatus;
import com.iortatechnxt.brokerverse.investment.domain.InstrumentType;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentHolding;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentPortfolio;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentPortfolioRepository;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentRun;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentTransaction;
import com.iortatechnxt.brokerverse.investment.domain.RunType;
import com.iortatechnxt.brokerverse.investment.domain.TransactionType;
import com.iortatechnxt.brokerverse.investment.service.HoldingEventService;
import com.iortatechnxt.brokerverse.investment.service.InvestmentRunService;
import com.iortatechnxt.brokerverse.investment.service.InvestmentService;
import com.iortatechnxt.brokerverse.investment.service.PortfolioService;
import com.iortatechnxt.brokerverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Investment life cycle against the demo chart; every test rolls back. */
@IntegrationTest
@Transactional
class InvestmentIT {

  private static final String MILLION = "1000000";

  @Autowired private InvestmentService holdings;
  @Autowired private HoldingEventService events;
  @Autowired private InvestmentRunService runs;
  @Autowired private PortfolioService portfolioService;
  @Autowired private InvestmentPortfolioRepository portfolios;
  @Autowired private LedgerQueryService ledger;
  @Autowired private ChartOfAccountsService accounts;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private Long company() {
    return data.company().getId();
  }

  private Long portfolio(String code) {
    return portfolios.findByCompanyIdAndCode(company(), code).orElseThrow().getId();
  }

  private BigDecimal balance(String account) {
    return ledger.netBalance(
        company(),
        accounts.getByCode(company(), account).getId(),
        null,
        LocalDate.of(2026, 12, 31));
  }

  private HoldingRequest request(Terms t) {
    return new HoldingRequest(
        company(),
        data.branch("HO").getId(),
        portfolio(t.portfolio()),
        t.type(),
        "T-" + t.settle(),
        "Test holding",
        "IS-BTR",
        "RoSS",
        "PHP",
        new BigDecimal(MILLION),
        new BigDecimal(t.price()),
        null,
        LocalDate.parse(t.settle()),
        LocalDate.parse(t.settle()),
        t.maturity() == null ? null : LocalDate.parse(t.maturity()),
        new BigDecimal(t.rate()),
        t.frequency(),
        t.dayCount(),
        t.method(),
        false,
        "1111",
        t.takeOn() != null,
        t.takeOn() == null ? null : LocalDate.parse(t.takeOn()));
  }

  private InvestmentHolding approved(Terms t) {
    InvestmentHolding h = as.run("accountant", () -> holdings.create(request(t)));
    return as.run("checker", () -> holdings.approve(h.getId()));
  }

  private InvestmentRun post(RunType type, int month) {
    return as.run("fmanager", () -> runs.post(company(), type, YearMonth.of(2026, month)));
  }

  @Test
  void couponBondAccruesOnThirty360AndCouponRelievesTheAccrual() {
    BigDecimal cashBefore = balance("1111");
    InvestmentHolding h =
        approved(
            new Terms(
                "AC-GOVT",
                InstrumentType.GOVERNMENT_BOND,
                MILLION,
                "2026-03-15",
                "2031-03-15",
                "6",
                CouponFrequency.SEMI_ANNUAL,
                DayCountConvention.THIRTY_360,
                null,
                null));
    assertThat(h.getStatus()).isEqualTo(HoldingStatus.ACTIVE);
    assertThat(h.terms().amortizationMethod()).isEqualTo(AmortizationMethod.NONE);
    assertThat(balance("1111")).isEqualByComparingTo(cashBefore.subtract(new BigDecimal(MILLION)));

    assertThat(runs.preview(company(), RunType.ACCRUAL, YearMonth.of(2026, 3)))
        .singleElement()
        .satisfies(p -> assertThat(p.due().amount()).isEqualByComparingTo("2500.00"));
    InvestmentRun march = post(RunType.ACCRUAL, 3);
    assertThat(post(RunType.ACCRUAL, 3).getId()).isEqualTo(march.getId());
    for (int month = 4; month <= 8; month++) {
      post(RunType.ACCRUAL, month);
    }
    assertThat(holdings.get(h.getId()).getAccruedInterest()).isEqualByComparingTo("27500.00");

    InvestmentTransaction coupon =
        as.run(
            "accountant",
            () ->
                events.receiveCoupon(
                    h.getId(),
                    new CouponReceiptRequest(
                        LocalDate.of(2026, 9, 15),
                        new BigDecimal("24000"),
                        new BigDecimal("6000"),
                        null)));

    assertThat(coupon.getGainLoss()).isZero();
    assertThat(coupon.getAccruedAfter()).isZero();
    assertThat(holdings.transactions(h.getId()))
        .extracting(InvestmentTransaction::getTxnType)
        .contains(TransactionType.PURCHASE, TransactionType.ACCRUAL, TransactionType.COUPON);
    assertThatThrownBy(
            () ->
                as.run(
                    "accountant",
                    () ->
                        events.receiveCoupon(
                            h.getId(),
                            new CouponReceiptRequest(
                                LocalDate.of(2026, 9, 15), BigDecimal.ONE, BigDecimal.ZERO, null))))
        .hasMessageContaining("already recorded");
    assertThat(runs.transactions(march.getId())).hasSize(1);
    assertThat(runs.findRun(company(), RunType.ACCRUAL, YearMonth.of(2026, 3))).isPresent();
    assertThat(runs.runs(company())).isNotEmpty();
  }

  @Test
  void treasuryBillDiscountIsAccretedAndMaturesWithoutGainOrLoss() {
    InvestmentHolding bill =
        approved(
            new Terms(
                "AC-GOVT",
                InstrumentType.TREASURY_BILL,
                "980000",
                "2026-03-02",
                "2026-08-31",
                "0",
                CouponFrequency.NONE,
                DayCountConvention.ACT_365,
                null,
                null));
    assertThat(bill.terms().amortizationMethod()).isEqualTo(AmortizationMethod.EFFECTIVE_INTEREST);
    assertThat(bill.getEffectiveRate()).isNotNull();
    for (int month = 3; month <= 7; month++) {
      post(RunType.AMORTIZATION, month);
    }
    BigDecimal carrying = holdings.get(bill.getId()).getAmortizedCost();
    assertThat(carrying).isBetween(new BigDecimal("995000"), new BigDecimal("999999"));
    assertThatThrownBy(
            () ->
                as.run(
                    "accountant",
                    () ->
                        events.mature(
                            bill.getId(),
                            new RedemptionRequest(
                                LocalDate.of(2026, 8, 30), BigDecimal.TEN, BigDecimal.ZERO, null))))
        .hasMessageContaining("not reached maturity");

    InvestmentTransaction matured =
        as.run(
            "accountant",
            () ->
                events.mature(
                    bill.getId(),
                    new RedemptionRequest(
                        LocalDate.of(2026, 8, 31),
                        new BigDecimal(MILLION),
                        BigDecimal.ZERO,
                        "Paid")));

    assertThat(matured.getAmount()).isEqualByComparingTo(MILLION);
    assertThat(matured.getGainLoss()).isZero();
    InvestmentHolding closed = holdings.get(bill.getId());
    assertThat(closed.getStatus()).isEqualTo(HoldingStatus.MATURED);
    assertThat(closed.carryingAmount()).isZero();
  }

  @Test
  void timeDepositAccruesActual365() {
    InvestmentHolding deposit =
        approved(
            new Terms(
                "AC-TD",
                InstrumentType.TIME_DEPOSIT,
                MILLION,
                "2026-06-01",
                "2027-06-01",
                "5",
                CouponFrequency.AT_MATURITY,
                DayCountConvention.ACT_365,
                null,
                null));
    InvestmentRun june = post(RunType.ACCRUAL, 6);
    assertThat(june.getTotalAmount()).isEqualByComparingTo("3972.60");
    assertThat(runs.preview(company(), RunType.AMORTIZATION, YearMonth.of(2026, 6))).isEmpty();
    assertThat(deposit.getPurchaseBatchNo()).isNotBlank();
  }

  @Test
  void premiumIsAmortizedStraightLine() {
    InvestmentHolding bond =
        approved(
            new Terms(
                "AC-GOVT",
                InstrumentType.GOVERNMENT_BOND,
                "1012000",
                "2026-06-30",
                "2027-06-30",
                "7",
                CouponFrequency.ANNUAL,
                DayCountConvention.ACT_365,
                AmortizationMethod.STRAIGHT_LINE,
                null));
    InvestmentRun july = post(RunType.AMORTIZATION, 7);
    assertThat(july.getTotalAmount()).isEqualByComparingTo("-1019.18");
    assertThat(holdings.get(bond.getId()).getAmortizedCost()).isEqualByComparingTo("1010980.82");
  }

  @Test
  void fvociSaleRecyclesTheReserveIntoTheRealizedGain() {
    InvestmentHolding bond =
        approved(
            new Terms(
                "FVOCI-DEBT",
                InstrumentType.CORPORATE_BOND,
                MILLION,
                "2026-07-01",
                "2029-07-01",
                "0",
                CouponFrequency.NONE,
                DayCountConvention.ACT_365,
                null,
                null));
    BigDecimal reserveBefore = balance("3400");
    InvestmentTransaction fv =
        as.run(
            "accountant",
            () ->
                events.remeasure(
                    bond.getId(),
                    new FairValueRequest(
                        LocalDate.of(2026, 7, 31), new BigDecimal("1020000"), "BVAL")));
    assertThat(fv.getAmount()).isEqualByComparingTo("20000.00");
    assertThat(balance("3400"))
        .isEqualByComparingTo(reserveBefore.subtract(new BigDecimal("20000")));
    InvestmentTransaction unchanged =
        as.run(
            "accountant",
            () ->
                events.remeasure(
                    bond.getId(),
                    new FairValueRequest(
                        LocalDate.of(2026, 8, 1), new BigDecimal("1020000"), null)));
    assertThat(unchanged.getBatchNo()).isNull();
    BigDecimal gainsBefore = balance("4503");

    InvestmentTransaction sale =
        as.run(
            "accountant",
            () ->
                events.sell(
                    bond.getId(),
                    new RedemptionRequest(
                        LocalDate.of(2026, 8, 15),
                        new BigDecimal("1030000"),
                        BigDecimal.ZERO,
                        null)));

    assertThat(sale.getGainLoss()).isEqualByComparingTo("30000.00");
    assertThat(balance("3400")).isEqualByComparingTo(reserveBefore);
    assertThat(balance("4503")).isEqualByComparingTo(gainsBefore.subtract(new BigDecimal("30000")));
    assertThat(holdings.get(bond.getId()).getStatus()).isEqualTo(HoldingStatus.SOLD);
  }

  @Test
  void takeOnHoldingOpensWithAccruedInterestSinceTheLastCoupon() {
    InvestmentHolding legacy =
        approved(
            new Terms(
                "AC-GOVT",
                InstrumentType.GOVERNMENT_BOND,
                MILLION,
                "2025-09-15",
                "2030-09-15",
                "6",
                CouponFrequency.SEMI_ANNUAL,
                DayCountConvention.THIRTY_360,
                null,
                "2026-01-01"));
    assertThat(legacy.getAccruedInterest()).isEqualByComparingTo("17666.67");
    assertThat(legacy.getAmortizedCost()).isEqualByComparingTo(MILLION);
    assertThat(holdings.transactions(legacy.getId()))
        .extracting(InvestmentTransaction::getTxnType)
        .containsExactly(TransactionType.TAKE_ON);
    assertThat(holdings.search(company(), HoldingStatus.ACTIVE, null, "T-2025")).isNotEmpty();
  }

  @Test
  void invalidOperationsAreRejected() {
    InvestmentHolding amortized =
        approved(
            new Terms(
                "AC-GOVT",
                InstrumentType.GOVERNMENT_BOND,
                MILLION,
                "2026-07-15",
                "2031-07-15",
                "6",
                CouponFrequency.SEMI_ANNUAL,
                DayCountConvention.THIRTY_360,
                null,
                null));
    FairValueRequest fv = new FairValueRequest(LocalDate.of(2026, 8, 1), BigDecimal.ONE, null);
    assertThatThrownBy(() -> as.run("accountant", () -> events.remeasure(amortized.getId(), fv)))
        .hasMessageContaining("not remeasured");
    RedemptionRequest late =
        new RedemptionRequest(LocalDate.of(2031, 7, 15), BigDecimal.ONE, BigDecimal.ZERO, null);
    assertThatThrownBy(() -> as.run("accountant", () -> events.sell(amortized.getId(), late)))
        .hasMessageContaining("has matured");
    CouponReceiptRequest early =
        new CouponReceiptRequest(LocalDate.of(2026, 7, 1), BigDecimal.ONE, BigDecimal.ZERO, null);
    assertThatThrownBy(
            () -> as.run("accountant", () -> events.receiveCoupon(amortized.getId(), early)))
        .hasMessageContaining("precedes");
    assertThatThrownBy(() -> as.run("checker", () -> holdings.approve(amortized.getId())))
        .hasMessageContaining("already approved");
    Terms noMaturity =
        new Terms(
            "AC-GOVT",
            InstrumentType.GOVERNMENT_BOND,
            MILLION,
            "2026-07-15",
            null,
            "6",
            CouponFrequency.SEMI_ANNUAL,
            DayCountConvention.THIRTY_360,
            null,
            null);
    assertThatThrownBy(() -> as.run("accountant", () -> holdings.create(request(noMaturity))))
        .hasMessageContaining("maturity date");
    Terms badTakeOn =
        new Terms(
            "AC-GOVT",
            InstrumentType.GOVERNMENT_BOND,
            MILLION,
            "2026-07-15",
            "2031-07-15",
            "6",
            CouponFrequency.SEMI_ANNUAL,
            DayCountConvention.THIRTY_360,
            null,
            "2026-07-01");
    assertThatThrownBy(() -> as.run("accountant", () -> holdings.create(request(badTakeOn))))
        .hasMessageContaining("take-on date");
    InvestmentHolding pending =
        as.run(
            "accountant",
            () ->
                holdings.create(
                    request(
                        new Terms(
                            "FVPL-EQ",
                            InstrumentType.EQUITY,
                            MILLION,
                            "2026-08-03",
                            null,
                            "0",
                            CouponFrequency.NONE,
                            DayCountConvention.ACT_365,
                            null,
                            null))));
    InvestmentHolding edited =
        as.run(
            "accountant",
            () ->
                holdings.update(
                    pending.getId(),
                    request(
                        new Terms(
                            "FVPL-EQ",
                            InstrumentType.EQUITY,
                            "990000",
                            "2026-08-03",
                            null,
                            "0",
                            CouponFrequency.NONE,
                            DayCountConvention.ACT_365,
                            null,
                            null))));
    assertThat(edited.getRecordStatus()).isEqualTo(RecordStatus.PENDING_AUTHORIZATION);
    assertThat(edited.terms().amortizationMethod()).isEqualTo(AmortizationMethod.NONE);
  }

  @Test
  void portfoliosFollowMakerCheckerAndNeedAFairValueAccount() {
    PortfolioRequest missing =
        new PortfolioRequest(
            company(), "T-PF", "Test", Classification.FVPL, "1501", "1504", "4501", "4503", null);
    assertThatThrownBy(() -> as.run("accountant", () -> portfolioService.create(missing)))
        .hasMessageContaining("fair value account");
    PortfolioRequest request =
        new PortfolioRequest(
            company(), "T-PF", "Test", Classification.FVPL, "1501", "1504", "4501", "4503", "4504");
    InvestmentPortfolio created = as.run("accountant", () -> portfolioService.create(request));
    InvestmentPortfolio authorized =
        as.run("checker", () -> portfolioService.authorize(created.getId()));
    assertThat(authorized.isActive()).isTrue();
    InvestmentPortfolio updated =
        as.run("accountant", () -> portfolioService.update(created.getId(), request));
    assertThat(updated.getRecordStatus()).isEqualTo(RecordStatus.PENDING_AUTHORIZATION);
    assertThat(portfolioService.list(company()))
        .extracting(InvestmentPortfolio::getCode)
        .contains("T-PF");
    PortfolioRequest bad =
        new PortfolioRequest(
            company(),
            "T-PF2",
            "Bad",
            Classification.AMORTIZED_COST,
            "1500",
            "1504",
            "4501",
            "4503",
            null);
    assertThatThrownBy(() -> as.run("accountant", () -> portfolioService.create(bad)))
        .hasMessageContaining("not postable");
  }

  /** Compact holding terms for the tests. */
  private record Terms(
      String portfolio,
      InstrumentType type,
      String price,
      String settle,
      String maturity,
      String rate,
      CouponFrequency frequency,
      DayCountConvention dayCount,
      AmortizationMethod method,
      String takeOn) {}
}
