package com.iortatechnxt.brokerverse.investment.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.investment.api.dto.HoldingRequest;
import com.iortatechnxt.brokerverse.investment.domain.AmortizationMethod;
import com.iortatechnxt.brokerverse.investment.domain.HoldingStatus;
import com.iortatechnxt.brokerverse.investment.domain.HoldingTerms;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentHolding;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentHoldingRepository;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentPortfolio;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentTransaction;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentTransactionRepository;
import com.iortatechnxt.brokerverse.investment.domain.TransactionType;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Investment holdings: capture (maker), approval (checker, posts the purchase or take-on), coupon
 * receipts, maturity, sale and fair value remeasurement. Month-end accrual and amortization runs
 * are in {@link InvestmentRunService}.
 */
@Service
@Transactional
public class InvestmentService {

  private static final String HOLDING = "InvestmentHolding";

  private final InvestmentHoldingRepository holdings;
  private final InvestmentTransactionRepository transactions;
  private final PortfolioService portfolios;
  private final InvestmentPostings postings;
  private final OrganizationService organization;
  private final PartyService parties;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param holdings holding repository
   * @param transactions transaction repository
   * @param portfolios portfolio service
   * @param postings posting helper
   * @param organization organization service
   * @param parties party service
   * @param numbers document numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public InvestmentService(
      InvestmentHoldingRepository holdings,
      InvestmentTransactionRepository transactions,
      PortfolioService portfolios,
      InvestmentPostings postings,
      OrganizationService organization,
      PartyService parties,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.holdings = holdings;
    this.transactions = transactions;
    this.portfolios = portfolios;
    this.postings = postings;
    this.organization = organization;
    this.parties = parties;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Searches holdings.
   *
   * @param companyId company
   * @param status status filter (null = all)
   * @param portfolioId portfolio filter (null = all)
   * @param term holding no. / security code / description fragment
   * @return holdings
   */
  @Transactional(readOnly = true)
  public List<InvestmentHolding> search(
      Long companyId, HoldingStatus status, Long portfolioId, String term) {
    return holdings.search(
        companyId,
        status == null ? EnumSet.allOf(HoldingStatus.class) : EnumSet.of(status),
        portfolioId,
        term == null ? "" : term.trim().toLowerCase(Locale.ROOT));
  }

  /**
   * Gets a holding.
   *
   * @param id id
   * @return holding
   */
  @Transactional(readOnly = true)
  public InvestmentHolding get(Long id) {
    return holdings.findById(id).orElseThrow(() -> new ResourceNotFoundException(HOLDING, id));
  }

  /**
   * Transaction history of a holding.
   *
   * @param id holding
   * @return transactions
   */
  @Transactional(readOnly = true)
  public List<InvestmentTransaction> transactions(Long id) {
    return transactions.findByHoldingIdOrderByTxnDateAscIdAsc(id);
  }

  /**
   * Captures a holding (pending approval).
   *
   * @param r request
   * @return holding
   */
  public InvestmentHolding create(HoldingRequest r) {
    InvestmentPortfolio portfolio = portfolios.get(r.portfolioId());
    if (!portfolio.isActive() || !portfolio.getCompanyId().equals(r.companyId())) {
      throw new BusinessRuleException(
          "INVALID_PORTFOLIO", "Portfolio " + portfolio.getCode() + " cannot be used");
    }
    String branchCode = organization.requireActiveBranch(r.branchId()).getCode();
    parties.requireActive(r.companyId(), r.issuerCode(), EnumSet.allOf(PartyType.class));
    String number = numbers.next("INV-" + branchCode + "-" + r.settlementDate().getYear());
    InvestmentHolding h =
        new InvestmentHolding(
            portfolio, r.branchId(), number, r.instrumentType(), r.issuerCode(), r.currency());
    apply(h, r);
    InvestmentHolding saved = holdings.save(h);
    audit.record(HOLDING, number, AuditAction.CREATE, "Captured " + r.description());
    return saved;
  }

  /**
   * Updates a holding awaiting approval (terms and description; not portfolio or issuer).
   *
   * @param id id
   * @param r request
   * @return holding
   */
  public InvestmentHolding update(Long id, HoldingRequest r) {
    InvestmentHolding h = get(id);
    apply(h, r);
    h.markModified();
    audit.record(HOLDING, h.getHoldingNo(), AuditAction.UPDATE, "Updated holding");
    return h;
  }

  /**
   * Approves a holding (checker) and posts the purchase, or the opening balance of a take-on
   * holding (amortized cost and accrued interest at the take-on date computed from its terms).
   *
   * @param id id
   * @return holding
   */
  public InvestmentHolding approve(Long id) {
    InvestmentHolding h = get(id);
    h.requirePending();
    HoldingTerms t = h.terms();
    boolean amortized = t.amortizationMethod() == AmortizationMethod.EFFECTIVE_INTEREST;
    BigDecimal rate =
        amortized
            ? InterestCalculator.effectiveRate(t, t.settlementDate(), t.purchasePrice())
            : null;
    BigDecimal carrying = t.purchasePrice();
    BigDecimal accrued = t.purchasedInterest();
    if (h.isTakeOn()) {
      LocalDate date = h.startDate();
      carrying =
          InterestCalculator.carryingAt(t, rate, t.purchasePrice(), t.settlementDate(), date);
      accrued =
          InterestCalculator.couponInterest(t, InterestCalculator.lastCouponDate(t, date), date);
    }
    h.approve(currentUser.username(), clock.instant(), carrying, accrued, rate);
    TransactionType type = h.isTakeOn() ? TransactionType.TAKE_ON : TransactionType.PURCHASE;
    InvestmentTransaction txn = new InvestmentTransaction(h, type, h.startDate(), carrying);
    BigDecimal total = carrying.add(accrued);
    txn.settle(h.isTakeOn() ? BigDecimal.ZERO : total, BigDecimal.ZERO, BigDecimal.ZERO);
    Map<String, BigDecimal> amounts =
        h.isTakeOn()
            ? Map.of("CARRYING_AMOUNT", carrying, "ACCRUED_INTEREST", accrued, "TOTAL", total)
            : Map.of("COST", carrying, "PURCHASED_INTEREST", accrued, "TOTAL", total);
    InvestmentTransaction posted =
        postings.post(
            txn, h.isTakeOn() ? "INVESTMENT_TAKE_ON" : "INVESTMENT_PURCHASE", type.name(), amounts);
    h.setPurchaseBatchNo(posted.getBatchNo());
    audit.record(
        HOLDING, h.getHoldingNo(), AuditAction.AUTHORIZE, "Approved, " + posted.getBatchNo());
    return h;
  }

  private void apply(InvestmentHolding h, HoldingRequest r) {
    portfolios.requirePostable(r.companyId(), r.bankAccount());
    validateDates(r);
    HoldingTerms draft =
        new HoldingTerms(
            r.faceValue(),
            r.purchasePrice(),
            BigDecimal.ZERO,
            r.tradeDate(),
            r.settlementDate(),
            r.instrumentType().isDebt() ? r.maturityDate() : null,
            r.instrumentType().isDebt() ? r.couponRate() : BigDecimal.ZERO,
            r.couponFrequency(),
            r.dayCount(),
            amortizationMethod(r));
    BigDecimal purchased =
        r.purchasedInterest() != null
            ? r.purchasedInterest()
            : InterestCalculator.couponInterest(
                draft,
                InterestCalculator.lastCouponDate(draft, r.settlementDate()),
                r.settlementDate());
    h.defineTerms(
        new HoldingTerms(
            draft.faceValue(),
            draft.purchasePrice(),
            Money.round(purchased),
            draft.tradeDate(),
            draft.settlementDate(),
            draft.maturityDate(),
            draft.couponRate(),
            draft.couponFrequency(),
            draft.dayCount(),
            draft.amortizationMethod()));
    if (r.takeOn()) {
      h.takeOnAt(r.takeOnDate());
    }
    h.setDescription(r.description());
    h.setSecurityCode(blankToNull(r.securityCode()));
    h.setCustodian(blankToNull(r.custodian()));
    h.setSecurityDeposit(r.securityDeposit());
    h.setBankAccount(r.bankAccount());
  }

  private static void validateDates(HoldingRequest r) {
    if (r.settlementDate().isBefore(r.tradeDate())) {
      throw new BusinessRuleException("INVALID_DATES", "Settlement precedes the trade date");
    }
    LocalDate maturity = r.maturityDate();
    if (r.instrumentType().isDebt() && !isAfter(maturity, r.settlementDate())) {
      throw new BusinessRuleException(
          "INVALID_MATURITY", "A debt instrument needs a maturity date after settlement");
    }
    if (r.takeOn() && !validTakeOn(r.takeOnDate(), r.settlementDate(), maturity)) {
      throw new BusinessRuleException(
          "INVALID_TAKE_ON_DATE", "The take-on date must fall between settlement and maturity");
    }
  }

  private static boolean validTakeOn(LocalDate takeOn, LocalDate settlement, LocalDate maturity) {
    return isAfter(takeOn, settlement) && (maturity == null || maturity.isAfter(takeOn));
  }

  private static boolean isAfter(LocalDate date, LocalDate reference) {
    return date != null && date.isAfter(reference);
  }

  private static AmortizationMethod amortizationMethod(HoldingRequest r) {
    if (!r.instrumentType().isDebt()) {
      return AmortizationMethod.NONE;
    }
    if (r.amortizationMethod() != null) {
      return r.amortizationMethod();
    }
    return r.faceValue().compareTo(r.purchasePrice()) == 0
        ? AmortizationMethod.NONE
        : AmortizationMethod.EFFECTIVE_INTEREST;
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
