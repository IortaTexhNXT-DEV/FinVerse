package com.iortatechnxt.finverse.investment.service;

import com.iortatechnxt.finverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.finverse.accounting.service.BusinessEvent;
import com.iortatechnxt.finverse.investment.domain.HoldingTerms;
import com.iortatechnxt.finverse.investment.domain.InvestmentHolding;
import com.iortatechnxt.finverse.investment.domain.InvestmentPortfolio;
import com.iortatechnxt.finverse.investment.domain.InvestmentTransaction;
import com.iortatechnxt.finverse.investment.domain.InvestmentTransactionRepository;
import com.iortatechnxt.finverse.investment.domain.TransactionType;
import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Posts investment events through the accounting engine and records them in the holding's
 * transaction history. The portfolio supplies the account roles INVESTMENT, ACCRUED_INTEREST,
 * INTEREST_INCOME, REALIZED_GAIN and FAIR_VALUE; the holding supplies BANK (its settlement
 * account).
 *
 * <p>Also computes and posts interest accrual and amortization up to a date, used by the month-end
 * runs and, as a catch-up, before coupon receipts, maturities, sales and fair value updates.
 */
@Component
public class InvestmentPostings {

  /** Source module recorded on every investment journal. */
  public static final String MODULE = "INVESTMENTS";

  private static final String KEY_PREFIX = "INV:";

  private final AccountingEventPublisher publisher;
  private final InvestmentTransactionRepository transactions;

  /**
   * Creates the helper.
   *
   * @param publisher accounting engine
   * @param transactions transaction repository
   */
  public InvestmentPostings(
      AccountingEventPublisher publisher, InvestmentTransactionRepository transactions) {
    this.publisher = publisher;
    this.transactions = transactions;
  }

  /**
   * Posts an event of a holding.
   *
   * @param h holding
   * @param eventType event type code
   * @param date value date
   * @param key suffix of the idempotency key (unique per holding)
   * @param amounts amount components
   * @return posted journal
   */
  public JournalBatch publish(
      InvestmentHolding h,
      String eventType,
      LocalDate date,
      String key,
      Map<String, BigDecimal> amounts) {
    InvestmentPortfolio p = h.getPortfolio();
    Map<String, String> roles = new HashMap<>();
    roles.put("INVESTMENT", p.getInvestmentAccount());
    roles.put("ACCRUED_INTEREST", p.getAccruedInterestAccount());
    roles.put("INTEREST_INCOME", p.getInterestIncomeAccount());
    roles.put("REALIZED_GAIN", p.getRealizedGainAccount());
    roles.put("BANK", h.getBankAccount());
    if (p.getFairValueAccount() != null) {
      roles.put("FAIR_VALUE", p.getFairValueAccount());
    }
    return publisher.publish(
        new BusinessEvent(
            eventType,
            h.getCompanyId(),
            h.getBranchId(),
            date,
            h.getCurrency(),
            MODULE,
            KEY_PREFIX + h.getId() + ":" + key,
            h.getHoldingNo(),
            h.getIssuerCode(),
            null,
            null,
            h.getDescription(),
            amounts,
            roles));
  }

  /**
   * Records and posts a transaction.
   *
   * @param txn transaction (holding already updated)
   * @param eventType event type
   * @param key idempotency key suffix
   * @param amounts amount components
   * @return saved transaction
   */
  public InvestmentTransaction post(
      InvestmentTransaction txn, String eventType, String key, Map<String, BigDecimal> amounts) {
    JournalBatch batch = publish(txn.getHolding(), eventType, txn.getTxnDate(), key, amounts);
    txn.posted(batch.getBatchNo());
    return transactions.save(txn);
  }

  /**
   * Coupon interest due from the last accrual up to a date (capped at maturity).
   *
   * @param h holding
   * @param date accrue up to
   * @return due interest, or null when nothing is due
   */
  public static Due accrualDue(InvestmentHolding h, LocalDate date) {
    HoldingTerms t = h.terms();
    LocalDate to = cap(t, date);
    LocalDate from = h.getLastAccrualDate();
    if (!to.isAfter(from)) {
      return null;
    }
    BigDecimal interest = InterestCalculator.couponInterest(t, from, to);
    return interest.signum() == 0 ? null : new Due(from, to, t.dayCount().days(from, to), interest);
  }

  /**
   * Amortization due from the last amortization up to a date (capped at maturity).
   *
   * @param h holding
   * @param date amortize up to
   * @return due amortization, or null when nothing is due
   */
  public static Due amortizationDue(InvestmentHolding h, LocalDate date) {
    HoldingTerms t = h.terms();
    LocalDate to = cap(t, date);
    LocalDate from = h.getLastAmortizationDate();
    if (!to.isAfter(from)) {
      return null;
    }
    BigDecimal amount =
        InterestCalculator.amortization(t, h.getEffectiveRate(), h.getAmortizedCost(), from, to);
    return amount.signum() == 0
        ? null
        : new Due(from, to, (int) ChronoUnit.DAYS.between(from, to), amount);
  }

  /**
   * Accrues and posts interest up to a date.
   *
   * @param h holding
   * @param date accrue up to
   * @param runId month-end run (null for a catch-up)
   * @return transaction, or null when nothing was due
   */
  public InvestmentTransaction accrue(InvestmentHolding h, LocalDate date, Long runId) {
    Due due = accrualDue(h, date);
    if (due == null) {
      return null;
    }
    h.accrue(due.to(), due.amount());
    return post(
        transaction(h, TransactionType.ACCRUAL, due, runId),
        "INVESTMENT_INTEREST_ACCRUAL",
        "ACR:" + due.to(),
        Map.of("INTEREST", due.amount()));
  }

  /**
   * Amortizes and posts the premium or discount up to a date.
   *
   * @param h holding
   * @param date amortize up to
   * @param runId month-end run (null for a catch-up)
   * @return transaction, or null when nothing was due
   */
  public InvestmentTransaction amortize(InvestmentHolding h, LocalDate date, Long runId) {
    Due due = amortizationDue(h, date);
    if (due == null) {
      return null;
    }
    h.amortize(due.to(), due.amount());
    return post(
        transaction(h, TransactionType.AMORTIZATION, due, runId),
        "INVESTMENT_AMORTIZATION",
        "AMR:" + due.to(),
        Map.of("AMORTIZATION", due.amount()));
  }

  private static InvestmentTransaction transaction(
      InvestmentHolding h, TransactionType type, Due due, Long runId) {
    InvestmentTransaction txn = new InvestmentTransaction(h, type, due.to(), due.amount());
    txn.covering(due.from(), due.days());
    txn.setRunId(runId);
    return txn;
  }

  private static LocalDate cap(HoldingTerms t, LocalDate date) {
    return t.maturityDate() != null && t.maturityDate().isBefore(date) ? t.maturityDate() : date;
  }

  /**
   * An amount due for a period.
   *
   * @param from start (exclusive)
   * @param to end (inclusive)
   * @param days days in the period
   * @param amount amount
   */
  public record Due(LocalDate from, LocalDate to, int days, BigDecimal amount) {}
}
