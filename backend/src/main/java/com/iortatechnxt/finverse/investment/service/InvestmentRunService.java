package com.iortatechnxt.finverse.investment.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.investment.domain.HoldingStatus;
import com.iortatechnxt.finverse.investment.domain.InvestmentHolding;
import com.iortatechnxt.finverse.investment.domain.InvestmentHoldingRepository;
import com.iortatechnxt.finverse.investment.domain.InvestmentRun;
import com.iortatechnxt.finverse.investment.domain.InvestmentRunRepository;
import com.iortatechnxt.finverse.investment.domain.InvestmentTransaction;
import com.iortatechnxt.finverse.investment.domain.InvestmentTransactionRepository;
import com.iortatechnxt.finverse.investment.domain.RunType;
import com.iortatechnxt.finverse.investment.service.InvestmentPostings.Due;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Month-end investment runs: coupon interest accrual and premium / discount amortization, each
 * previewed and then posted once per period (posting a period again returns the existing run). Each
 * holding held at the period end is brought up to the period end (or its maturity date) with one
 * journal per holding, valued at that date.
 */
@Service
@Transactional
public class InvestmentRunService {

  private final InvestmentHoldingRepository holdings;
  private final InvestmentRunRepository runs;
  private final InvestmentTransactionRepository transactions;
  private final InvestmentPostings postings;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param holdings holding repository
   * @param runs run repository
   * @param transactions transaction repository
   * @param postings posting helper
   * @param audit audit trail
   */
  public InvestmentRunService(
      InvestmentHoldingRepository holdings,
      InvestmentRunRepository runs,
      InvestmentTransactionRepository transactions,
      InvestmentPostings postings,
      AuditTrailService audit) {
    this.holdings = holdings;
    this.runs = runs;
    this.transactions = transactions;
    this.postings = postings;
    this.audit = audit;
  }

  /**
   * Computes the amounts due for a period without posting.
   *
   * @param companyId company
   * @param type run type
   * @param period month
   * @return amounts per holding
   */
  @Transactional(readOnly = true)
  public List<Proposal> preview(Long companyId, RunType type, YearMonth period) {
    LocalDate end = period.atEndOfMonth();
    List<Proposal> proposals = new ArrayList<>();
    for (InvestmentHolding h :
        holdings.findByCompanyIdAndStatusOrderByHoldingNo(companyId, HoldingStatus.ACTIVE)) {
      Due due =
          type == RunType.ACCRUAL
              ? InvestmentPostings.accrualDue(h, end)
              : InvestmentPostings.amortizationDue(h, end);
      if (due != null) {
        proposals.add(new Proposal(h, due));
      }
    }
    return proposals;
  }

  /**
   * Posts a run (idempotent per company, type and period).
   *
   * @param companyId company
   * @param type run type
   * @param period month
   * @return run
   */
  public InvestmentRun post(Long companyId, RunType type, YearMonth period) {
    Optional<InvestmentRun> existing = findRun(companyId, type, period);
    if (existing.isPresent()) {
      return existing.get();
    }
    InvestmentRun run = runs.save(new InvestmentRun(companyId, type, period));
    for (Proposal p : preview(companyId, type, period)) {
      InvestmentTransaction txn =
          type == RunType.ACCRUAL
              ? postings.accrue(p.holding(), run.getPeriodEnd(), run.getId())
              : postings.amortize(p.holding(), run.getPeriodEnd(), run.getId());
      run.count(txn.getAmount());
    }
    audit.record(
        "InvestmentRun",
        type + ":" + period,
        AuditAction.CREATE,
        "Posted " + type + " " + period + ", total " + run.getTotalAmount().toPlainString());
    return run;
  }

  /**
   * Finds a posted run.
   *
   * @param companyId company
   * @param type type
   * @param period month
   * @return run if posted
   */
  @Transactional(readOnly = true)
  public Optional<InvestmentRun> findRun(Long companyId, RunType type, YearMonth period) {
    return runs.findByCompanyIdAndRunTypeAndPeriod(companyId, type, period.toString());
  }

  /**
   * Lists posted runs.
   *
   * @param companyId company
   * @return runs, newest period first
   */
  @Transactional(readOnly = true)
  public List<InvestmentRun> runs(Long companyId) {
    return runs.findByCompanyIdOrderByPeriodDescRunTypeAsc(companyId);
  }

  /**
   * Transactions posted by a run.
   *
   * @param runId run
   * @return transactions
   */
  @Transactional(readOnly = true)
  public List<InvestmentTransaction> transactions(Long runId) {
    if (!runs.existsById(runId)) {
      throw new ResourceNotFoundException("InvestmentRun", runId);
    }
    return transactions.findByRunIdOrderById(runId);
  }

  /**
   * An amount due for a holding.
   *
   * @param holding holding
   * @param due amount and period
   */
  public record Proposal(InvestmentHolding holding, Due due) {}
}
