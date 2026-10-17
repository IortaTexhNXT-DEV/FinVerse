package com.iortatechnxt.brokerverse.investment.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.investment.api.dto.CouponReceiptRequest;
import com.iortatechnxt.brokerverse.investment.api.dto.FairValueRequest;
import com.iortatechnxt.brokerverse.investment.api.dto.RedemptionRequest;
import com.iortatechnxt.brokerverse.investment.domain.Classification;
import com.iortatechnxt.brokerverse.investment.domain.HoldingStatus;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentHolding;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentTransaction;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentTransactionRepository;
import com.iortatechnxt.brokerverse.investment.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Events of a held investment: coupon receipts, maturity, sale and fair value remeasurement. Each
 * first brings interest (and amortization) up to its date, then posts through the accounting
 * engine.
 */
@Service
@Transactional
public class HoldingEventService {

  private static final String HOLDING = "InvestmentHolding";
  private static final String ACCRUED_INTEREST = "ACCRUED_INTEREST";
  private static final String FINAL_TAX = "FINAL_TAX";

  private final InvestmentService holdings;
  private final InvestmentTransactionRepository transactions;
  private final InvestmentPostings postings;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param holdings holding service
   * @param transactions transaction repository
   * @param postings posting helper
   * @param audit audit trail
   * @param clock clock
   */
  public HoldingEventService(
      InvestmentService holdings,
      InvestmentTransactionRepository transactions,
      InvestmentPostings postings,
      AuditTrailService audit,
      Clock clock) {
    this.holdings = holdings;
    this.transactions = transactions;
    this.postings = postings;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Records a coupon / interest receipt: accrues interest up to the receipt date, relieves the
   * accrued interest receivable and books the difference to interest income.
   *
   * @param id holding
   * @param r receipt
   * @return transaction
   */
  public InvestmentTransaction receiveCoupon(Long id, CouponReceiptRequest r) {
    InvestmentHolding h = holdings.get(id);
    h.requireActive();
    requireOnOrAfterStart(h, r.receiptDate());
    if (transactions.existsByHoldingIdAndTxnTypeAndTxnDate(
        id, TransactionType.COUPON, r.receiptDate())) {
      throw new BusinessRuleException(
          "DUPLICATE_COUPON", "A coupon is already recorded on " + r.receiptDate());
    }
    postings.accrue(h, r.receiptDate(), null);
    BigDecimal accrued = h.relieveAccruedInterest();
    BigDecimal gross = r.cashAmount().add(r.finalTax());
    BigDecimal adjustment = gross.subtract(accrued);
    InvestmentTransaction txn =
        new InvestmentTransaction(h, TransactionType.COUPON, r.receiptDate(), gross);
    txn.settle(r.cashAmount(), r.finalTax(), adjustment);
    txn.setRemarks(r.remarks());
    return postings.post(
        txn,
        "INVESTMENT_INTEREST_RECEIPT",
        "CPN:" + r.receiptDate(),
        Map.of(
            "CASH",
            r.cashAmount(),
            FINAL_TAX,
            r.finalTax(),
            ACCRUED_INTEREST,
            accrued,
            "INCOME_ADJUSTMENT",
            adjustment));
  }

  /**
   * Redeems a holding at maturity.
   *
   * @param id holding
   * @param r redemption (value date on or after the maturity date)
   * @return transaction
   */
  public InvestmentTransaction mature(Long id, RedemptionRequest r) {
    InvestmentHolding h = holdings.get(id);
    h.requireActive();
    LocalDate maturity = h.getMaturityDate();
    if (maturity == null || r.valueDate().isBefore(maturity)) {
      throw new BusinessRuleException(
          "NOT_MATURED", "Holding " + h.getHoldingNo() + " has not reached maturity");
    }
    return derecognize(h, TransactionType.MATURITY, r);
  }

  /**
   * Sells a holding before maturity.
   *
   * @param id holding
   * @param r sale
   * @return transaction
   */
  public InvestmentTransaction sell(Long id, RedemptionRequest r) {
    InvestmentHolding h = holdings.get(id);
    h.requireActive();
    requireOnOrAfterStart(h, r.valueDate());
    if (h.getMaturityDate() != null && !r.valueDate().isBefore(h.getMaturityDate())) {
      throw new BusinessRuleException(
          "ALREADY_MATURED", "Holding " + h.getHoldingNo() + " has matured; record the maturity");
    }
    return derecognize(h, TransactionType.SALE, r);
  }

  /**
   * Remeasures an FVOCI / FVPL holding to fair value after bringing amortization up to the date.
   *
   * @param id holding
   * @param r valuation
   * @return transaction
   */
  public InvestmentTransaction remeasure(Long id, FairValueRequest r) {
    InvestmentHolding h = holdings.get(id);
    h.requireActive();
    requireOnOrAfterStart(h, r.valuationDate());
    if (!h.getPortfolio().getClassification().isFairValued()) {
      throw new BusinessRuleException(
          "NOT_FAIR_VALUED", "Holdings at amortized cost are not remeasured to fair value");
    }
    postings.amortize(h, r.valuationDate(), null);
    BigDecimal change = r.fairValue().subtract(h.carryingAmount());
    h.remeasure(r.valuationDate(), r.fairValue(), change);
    InvestmentTransaction txn =
        new InvestmentTransaction(h, TransactionType.FAIR_VALUE, r.valuationDate(), change);
    txn.setRemarks(r.remarks());
    if (change.signum() == 0) {
      txn.posted(null);
      return transactions.save(txn);
    }
    return postings.post(
        txn,
        "INVESTMENT_FAIR_VALUE",
        "FV:" + r.valuationDate() + ":" + clock.millis(),
        Map.of("FAIR_VALUE_CHANGE", change));
  }

  private InvestmentTransaction derecognize(
      InvestmentHolding h, TransactionType type, RedemptionRequest r) {
    postings.accrue(h, r.valueDate(), null);
    postings.amortize(h, r.valueDate(), null);
    BigDecimal carrying = h.carryingAmount();
    BigDecimal reserve =
        h.getPortfolio().getClassification() == Classification.FVOCI
            ? h.getFairValueAdjustment()
            : BigDecimal.ZERO;
    BigDecimal accrued = h.relieveAccruedInterest();
    BigDecimal gain =
        r.proceeds().add(r.finalTax()).add(reserve).subtract(carrying).subtract(accrued);
    h.close(
        type == TransactionType.MATURITY ? HoldingStatus.MATURED : HoldingStatus.SOLD,
        r.valueDate());
    InvestmentTransaction txn = new InvestmentTransaction(h, type, r.valueDate(), carrying);
    txn.settle(r.proceeds(), r.finalTax(), gain);
    txn.setRemarks(r.remarks());
    audit.record(
        HOLDING,
        h.getHoldingNo(),
        AuditAction.UPDATE,
        type + ", gain/(loss) " + gain.toPlainString());
    return postings.post(
        txn,
        type == TransactionType.MATURITY ? "INVESTMENT_MATURITY" : "INVESTMENT_SALE",
        type.name(),
        Map.of(
            "PROCEEDS",
            r.proceeds(),
            FINAL_TAX,
            r.finalTax(),
            "FV_RESERVE",
            reserve,
            "CARRYING_AMOUNT",
            carrying,
            ACCRUED_INTEREST,
            accrued,
            "REALIZED_GAIN",
            gain));
  }

  private static void requireOnOrAfterStart(InvestmentHolding h, LocalDate date) {
    if (date.isBefore(h.startDate())) {
      throw new BusinessRuleException(
          "DATE_BEFORE_START", "Date " + date + " precedes holding " + h.getHoldingNo());
    }
  }
}
