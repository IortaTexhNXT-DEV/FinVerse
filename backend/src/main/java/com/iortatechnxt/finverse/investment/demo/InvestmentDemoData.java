package com.iortatechnxt.finverse.investment.demo;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.investment.api.dto.CouponReceiptRequest;
import com.iortatechnxt.finverse.investment.api.dto.FairValueRequest;
import com.iortatechnxt.finverse.investment.api.dto.HoldingRequest;
import com.iortatechnxt.finverse.investment.api.dto.RedemptionRequest;
import com.iortatechnxt.finverse.investment.domain.CouponFrequency;
import com.iortatechnxt.finverse.investment.domain.DayCountConvention;
import com.iortatechnxt.finverse.investment.domain.HoldingStatus;
import com.iortatechnxt.finverse.investment.domain.HoldingTerms;
import com.iortatechnxt.finverse.investment.domain.InstrumentType;
import com.iortatechnxt.finverse.investment.domain.InvestmentHolding;
import com.iortatechnxt.finverse.investment.domain.InvestmentHoldingRepository;
import com.iortatechnxt.finverse.investment.domain.InvestmentPortfolioRepository;
import com.iortatechnxt.finverse.investment.domain.InvestmentTransactionRepository;
import com.iortatechnxt.finverse.investment.domain.RunType;
import com.iortatechnxt.finverse.investment.domain.TransactionType;
import com.iortatechnxt.finverse.investment.service.HoldingEventService;
import com.iortatechnxt.finverse.investment.service.InterestCalculator;
import com.iortatechnxt.finverse.investment.service.InvestmentRunService;
import com.iortatechnxt.finverse.investment.service.InvestmentService;
import com.iortatechnxt.finverse.organization.domain.BranchRepository;
import com.iortatechnxt.finverse.organization.domain.Company;
import com.iortatechnxt.finverse.organization.domain.CompanyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Demo profile only: builds an investment portfolio for the demo company through the services.
 * Holdings bought before 2026 are taken on at 2026-01-01; 2026 purchases settle in January to July.
 * Month by month (January to September 2026) coupons are received (net of 20 % final tax), two
 * holdings mature, an equity is sold, FVOCI / FVPL holdings are revalued at 30 June, and the
 * accrual and amortization runs are posted. Idempotent: every step is skipped when already done.
 */
@Component
@Profile("demo")
@Order(70)
public class InvestmentDemoData implements ApplicationRunner {

  private static final String COMPANY = "FVI";
  private static final LocalDate TAKE_ON = LocalDate.of(2026, 1, 1);
  private static final YearMonth FIRST = YearMonth.of(2026, 1);
  private static final YearMonth LAST = YearMonth.of(2026, 9);
  private static final LocalDate VALUATION = LocalDate.of(2026, 6, 30);
  private static final LocalDate SALE_DATE = LocalDate.of(2026, 9, 10);
  private static final String SOLD = "EQ-ALI-2026";
  private static final BigDecimal FINAL_TAX = new BigDecimal("0.20");
  private static final String MAKER = "accountant";
  private static final String CHECKER = "checker";
  private static final String MANAGER = "fmanager";
  private static final Map<String, String> FAIR_VALUES =
      Map.of("CB-ALI-2028", "10050000", "CB-SMIC-2029", "8020000", "EQ-SM-2025", "4725000");

  /** Security code | description | portfolio | instrument | issuer | custodian (- = none). */
  private static final String IDENTITIES =
      """
      GS-FXTN-0529|FXTN 5-year bond 6.25% 2029|AC-GOVT|GOVERNMENT_BOND|IS-BTR|BTr RoSS
      SD-FXTN-1030|FXTN 6.875% 2030 (security deposit)|SEC-DEP|GOVERNMENT_BOND|IS-BTR|Insurance Commission
      CB-ALI-2028|Ayala Land 5.90% bond 2028|FVOCI-DEBT|CORPORATE_BOND|IS-ALI|BDO Trust
      EQ-SM-2025|SM Investments common shares|FVPL-EQ|EQUITY|IS-SMIC|PDTC
      TD-BPI-2025|BPI 182-day time deposit 5.25%|AC-TD|TIME_DEPOSIT|BK-BPI|-
      TB-364-2026A|364-day Treasury bill 2027|AC-GOVT|TREASURY_BILL|IS-BTR|BTr RoSS
      TB-182-2026B|182-day Treasury bill August 2026|AC-GOVT|TREASURY_BILL|IS-BTR|BTr RoSS
      TD-BDO-2026|BDO 274-day time deposit 5.50%|AC-TD|TIME_DEPOSIT|BK-BDO|-
      GS-RTB-2031|Retail Treasury Bond 6.50% 2031|AC-GOVT|GOVERNMENT_BOND|IS-BTR|BTr RoSS
      CB-SMIC-2029|SMIC 6.00% bond 2029|FVOCI-DEBT|CORPORATE_BOND|IS-SMIC|BDO Trust
      TD-LBP-2026|Land Bank 1-year time deposit 5.75%|AC-TD|TIME_DEPOSIT|BK-LBP|-
      EQ-ALI-2026|Ayala Land common shares|FVPL-EQ|EQUITY|IS-ALI|PDTC
      """;

  /**
   * Security code | face | price | settlement | maturity (- = none) | coupon % | frequency | day
   * count | security deposit (same order as the identities).
   */
  private static final String TERMS =
      """
      GS-FXTN-0529|20000000|19640000|2024-03-15|2029-03-15|6.25|SEMI_ANNUAL|THIRTY_360|false
      SD-FXTN-1030|25000000|25300000|2023-10-12|2030-10-12|6.875|SEMI_ANNUAL|THIRTY_360|true
      CB-ALI-2028|10000000|9950000|2025-06-27|2028-06-27|5.90|QUARTERLY|ACT_365|false
      EQ-SM-2025|4500000|4500000|2025-09-15|-|0|NONE|ACT_365|false
      TD-BPI-2025|15000000|15000000|2025-11-17|2026-05-18|5.25|AT_MATURITY|ACT_365|false
      TB-364-2026A|10000000|9450000|2026-01-14|2027-01-13|0|NONE|ACT_365|false
      TB-182-2026B|5000000|4870000|2026-02-11|2026-08-12|0|NONE|ACT_365|false
      TD-BDO-2026|20000000|20000000|2026-03-02|2026-12-01|5.50|AT_MATURITY|ACT_365|false
      GS-RTB-2031|15000000|15150000|2026-03-25|2031-03-25|6.50|QUARTERLY|THIRTY_360|false
      CB-SMIC-2029|8000000|8040000|2026-05-05|2029-10-20|6.00|SEMI_ANNUAL|ACT_365|false
      TD-LBP-2026|10000000|10000000|2026-06-01|2027-06-01|5.75|AT_MATURITY|ACT_365|false
      EQ-ALI-2026|3000000|3000000|2026-07-08|-|0|NONE|ACT_365|false
      """;

  private static final List<DemoHolding> HOLDINGS = table(IDENTITIES, TERMS);

  private final CompanyRepository companies;
  private final BranchRepository branches;
  private final InvestmentPortfolioRepository portfolios;
  private final InvestmentHoldingRepository holdings;
  private final InvestmentTransactionRepository transactions;
  private final InvestmentService service;
  private final HoldingEventService holdingEvents;
  private final InvestmentRunService runs;

  /**
   * Creates the runner.
   *
   * @param companies companies
   * @param branches branches
   * @param portfolios portfolios
   * @param holdings holdings
   * @param transactions holding transactions
   * @param service investment service
   * @param events holding event service
   * @param runs run service
   */
  public InvestmentDemoData(
      CompanyRepository companies,
      BranchRepository branches,
      InvestmentPortfolioRepository portfolios,
      InvestmentHoldingRepository holdings,
      InvestmentTransactionRepository transactions,
      InvestmentService service,
      HoldingEventService events,
      InvestmentRunService runs) {
    this.companies = companies;
    this.branches = branches;
    this.portfolios = portfolios;
    this.holdings = holdings;
    this.transactions = transactions;
    this.service = service;
    this.holdingEvents = events;
    this.runs = runs;
  }

  @Override
  public void run(ApplicationArguments args) {
    Optional<Company> company = companies.findByCode(COMPANY);
    if (company.isEmpty()
        || portfolios.findByCompanyIdAndCode(company.get().getId(), "AC-GOVT").isEmpty()) {
      return;
    }
    Long companyId = company.get().getId();
    for (YearMonth month = FIRST; !month.isAfter(LAST); month = month.plusMonths(1)) {
      YearMonth period = month;
      HOLDINGS.stream()
          .filter(d -> YearMonth.from(start(d)).equals(period))
          .forEach(d -> acquire(companyId, d));
      List<DemoEvent> events = new ArrayList<>();
      HOLDINGS.forEach(d -> events.addAll(events(companyId, d, period)));
      events.sort(Comparator.comparing(DemoEvent::date));
      events.forEach(e -> as(MAKER, e.action()));
      as(MANAGER, () -> runs.post(companyId, RunType.ACCRUAL, period));
      as(MANAGER, () -> runs.post(companyId, RunType.AMORTIZATION, period));
    }
  }

  private void acquire(Long companyId, DemoHolding d) {
    InvestmentHolding h =
        holdings
            .findFirstByCompanyIdAndSecurityCode(companyId, d.code())
            .orElseGet(() -> as(MAKER, () -> service.create(request(companyId, d))));
    if (h.getStatus() == HoldingStatus.PENDING_APPROVAL) {
      as(CHECKER, () -> service.approve(h.getId()));
    }
  }

  private List<DemoEvent> events(Long companyId, DemoHolding d, YearMonth month) {
    Optional<InvestmentHolding> found =
        holdings.findFirstByCompanyIdAndSecurityCode(companyId, d.code());
    if (found.isEmpty() || found.get().getStatus() != HoldingStatus.ACTIVE) {
      return List.of();
    }
    InvestmentHolding h = found.get();
    List<DemoEvent> events = new ArrayList<>(couponEvents(h, month));
    LocalDate maturity = h.getMaturityDate();
    if (maturity != null && YearMonth.from(maturity).equals(month)) {
      events.add(new DemoEvent(maturity, () -> mature(h)));
    }
    if (needsValuation(h, d, month)) {
      BigDecimal value = new BigDecimal(FAIR_VALUES.get(d.code()));
      events.add(
          new DemoEvent(
              VALUATION,
              () ->
                  holdingEvents.remeasure(
                      h.getId(), new FairValueRequest(VALUATION, value, "Bloomberg BVAL close"))));
    }
    if (SOLD.equals(d.code()) && month.equals(YearMonth.from(SALE_DATE))) {
      RedemptionRequest sale =
          new RedemptionRequest(SALE_DATE, new BigDecimal("3180000.00"), BigDecimal.ZERO, "Sale");
      events.add(new DemoEvent(SALE_DATE, () -> holdingEvents.sell(h.getId(), sale)));
    }
    return events;
  }

  private boolean needsValuation(InvestmentHolding h, DemoHolding d, YearMonth month) {
    return month.equals(YearMonth.from(VALUATION))
        && FAIR_VALUES.containsKey(d.code())
        && !transactions.existsByHoldingIdAndTxnTypeAndTxnDate(
            h.getId(), TransactionType.FAIR_VALUE, VALUATION);
  }

  private List<DemoEvent> couponEvents(InvestmentHolding h, YearMonth month) {
    HoldingTerms t = h.terms();
    if (t.couponFrequency().months() == 0) {
      return List.of();
    }
    return InterestCalculator.couponDates(t, h.startDate(), month.atEndOfMonth()).stream()
        .filter(date -> YearMonth.from(date).equals(month))
        .filter(
            date ->
                !transactions.existsByHoldingIdAndTxnTypeAndTxnDate(
                    h.getId(), TransactionType.COUPON, date))
        .map(date -> new DemoEvent(date, () -> coupon(h, date)))
        .toList();
  }

  private Object coupon(InvestmentHolding h, LocalDate date) {
    HoldingTerms t = h.terms();
    LocalDate previous = InterestCalculator.lastCouponDate(t, date.minusDays(1));
    BigDecimal gross = InterestCalculator.couponInterest(t, previous, date);
    BigDecimal tax = Money.round(gross.multiply(FINAL_TAX));
    return holdingEvents.receiveCoupon(
        h.getId(), new CouponReceiptRequest(date, gross.subtract(tax), tax, "Coupon " + date));
  }

  private Object mature(InvestmentHolding h) {
    HoldingTerms t = h.terms();
    BigDecimal interest =
        t.couponFrequency() == CouponFrequency.AT_MATURITY
            ? InterestCalculator.couponInterest(t, t.settlementDate(), t.maturityDate())
            : BigDecimal.ZERO;
    BigDecimal tax = Money.round(interest.multiply(FINAL_TAX));
    return holdingEvents.mature(
        h.getId(),
        new RedemptionRequest(
            t.maturityDate(), t.faceValue().add(interest).subtract(tax), tax, "Matured"));
  }

  private HoldingRequest request(Long companyId, DemoHolding d) {
    LocalDate settlement = LocalDate.parse(d.settlement());
    boolean takeOn = settlement.isBefore(TAKE_ON);
    return new HoldingRequest(
        companyId,
        branches.findByCompanyIdAndCode(companyId, "HO").orElseThrow().getId(),
        portfolios.findByCompanyIdAndCode(companyId, d.portfolio()).orElseThrow().getId(),
        d.type(),
        d.code(),
        d.description(),
        d.issuer(),
        d.custodian(),
        "PHP",
        new BigDecimal(d.face()),
        new BigDecimal(d.price()),
        null,
        settlement,
        settlement,
        d.maturity() == null ? null : LocalDate.parse(d.maturity()),
        new BigDecimal(d.rate()),
        d.frequency(),
        d.dayCount(),
        null,
        d.deposit(),
        "1111",
        takeOn,
        takeOn ? TAKE_ON : null);
  }

  private static List<DemoHolding> table(String identities, String terms) {
    List<String> left = identities.lines().toList();
    List<String> right = terms.lines().toList();
    List<DemoHolding> rows = new ArrayList<>();
    for (int i = 0; i < left.size(); i++) {
      String termsLine = right.get(i);
      String line = left.get(i) + termsLine.substring(termsLine.indexOf('|'));
      rows.add(DemoHolding.parse(line.split("\\|")));
    }
    return List.copyOf(rows);
  }

  private static LocalDate start(DemoHolding d) {
    LocalDate settlement = LocalDate.parse(d.settlement());
    return settlement.isBefore(TAKE_ON) ? TAKE_ON : settlement;
  }

  private static <T> T as(String user, Supplier<T> action) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    try {
      return action.get();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  /** A dated demo event, executed in date order within its month. */
  private record DemoEvent(LocalDate date, Supplier<Object> action) {}

  /** A demo holding definition. */
  private record DemoHolding(
      String code,
      String description,
      String portfolio,
      InstrumentType type,
      String issuer,
      String custodian,
      String face,
      String price,
      String settlement,
      String maturity,
      String rate,
      CouponFrequency frequency,
      DayCountConvention dayCount,
      boolean deposit) {

    /** Parses a table line; fields are consumed left to right. */
    static DemoHolding parse(String[] fields) {
      Iterator<String> f = List.of(fields).iterator();
      return new DemoHolding(
          f.next(),
          f.next(),
          f.next(),
          InstrumentType.valueOf(f.next()),
          f.next(),
          none(f.next()),
          f.next(),
          f.next(),
          f.next(),
          none(f.next()),
          f.next(),
          CouponFrequency.valueOf(f.next()),
          DayCountConvention.valueOf(f.next()),
          Boolean.parseBoolean(f.next()));
    }

    private static String none(String value) {
      return "-".equals(value) ? null : value;
    }
  }
}
