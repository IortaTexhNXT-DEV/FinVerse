package com.iortatechnxt.finverse.reinsurance.service;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.reinsurance.domain.Cession;
import com.iortatechnxt.finverse.reinsurance.domain.CessionKey;
import com.iortatechnxt.finverse.reinsurance.domain.CessionRepository;
import com.iortatechnxt.finverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.finverse.system.domain.JobRun;
import com.iortatechnxt.finverse.system.domain.JobTrigger;
import com.iortatechnxt.finverse.system.service.JobOutcome;
import com.iortatechnxt.finverse.system.service.JobRunService;
import com.iortatechnxt.finverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.finverse.underwriting.service.PremiumTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The RI allocation run: cedes, in one batch, the approved premium transactions of a period that
 * are not yet ceded. A preview shows the planned split without posting. Posting runs as a recorded
 * job ({@value #JOB_NAME}) with one database transaction per premium transaction, so one failure
 * (for example a closed period) does not stop the others; failures are reported in the result.
 */
@Service
public class AllocationRunService {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "RI_ALLOCATION";

  private static final int MAX_MESSAGES = 20;

  private final PolicyQueryService policies;
  private final CessionRepository cessions;
  private final CessionPlanner planner;
  private final CessionService cessionService;
  private final JobRunService jobRuns;
  private final TransactionTemplate readOnly;
  private final TransactionTemplate perTransaction;

  /**
   * Creates the service.
   *
   * @param policies approved premium transactions
   * @param cessions ceded transactions
   * @param planner allocation planner
   * @param cessionService cession posting
   * @param jobRuns job run registry
   * @param transactionManager transaction manager
   */
  public AllocationRunService(
      PolicyQueryService policies,
      CessionRepository cessions,
      CessionPlanner planner,
      CessionService cessionService,
      JobRunService jobRuns,
      PlatformTransactionManager transactionManager) {
    this.policies = policies;
    this.cessions = cessions;
    this.planner = planner;
    this.cessionService = cessionService;
    this.jobRuns = jobRuns;
    this.readOnly = new TransactionTemplate(transactionManager);
    this.readOnly.setReadOnly(true);
    this.perTransaction = new TransactionTemplate(transactionManager);
  }

  /**
   * Planned allocation of the transactions of a period not yet ceded.
   *
   * @param companyId company
   * @param from first approval date
   * @param to last approval date
   * @return one row per transaction
   */
  public List<AllocationPreviewRow> preview(Long companyId, LocalDate from, LocalDate to) {
    return readOnly.execute(
        s -> {
          List<AllocationPreviewRow> rows =
              pending(companyId, from, to).stream().map(this::previewRow).toList();
          // A preview writes nothing: roll back explicitly, as a transaction that could not be
          // planned has marked the transaction rollback-only and a commit would then fail.
          s.setRollbackOnly();
          return rows;
        });
  }

  /**
   * Cedes the transactions of a period not yet ceded, as a recorded job run.
   *
   * @param companyId company
   * @param from first approval date
   * @param to last approval date
   * @param trigger manual (screen) or scheduled
   * @return result
   */
  public AllocationRunResult post(
      Long companyId, LocalDate from, LocalDate to, JobTrigger trigger) {
    RunTally tally = new RunTally();
    JobRun run =
        jobRuns.execute(
            JOB_NAME,
            trigger,
            () -> {
              cedeAll(companyId, from, to, tally);
              return tally.outcome();
            });
    return new AllocationRunResult(
        run.getId(),
        run.getStatus().name(),
        tally.ceded,
        tally.failed,
        tally.premium,
        List.copyOf(tally.messages));
  }

  /**
   * Cedes the pending transactions of a period (used by the scheduled job).
   *
   * @param companyId company
   * @param from first approval date
   * @param to last approval date
   * @return job outcome
   */
  JobOutcome cedePeriod(Long companyId, LocalDate from, LocalDate to) {
    RunTally tally = new RunTally();
    cedeAll(companyId, from, to, tally);
    return tally.outcome();
  }

  private void cedeAll(Long companyId, LocalDate from, LocalDate to, RunTally tally) {
    List<PremiumTransaction> pending =
        Objects.requireNonNullElse(readOnly.execute(s -> pending(companyId, from, to)), List.of());
    for (PremiumTransaction txn : pending) {
      try {
        tally.success(
            Objects.requireNonNull(perTransaction.execute(s -> cessionService.cede(txn))));
      } catch (RuntimeException ex) {
        tally.failure(txn.documentNo() + ": " + ex.getMessage());
      }
    }
  }

  private List<PremiumTransaction> pending(Long companyId, LocalDate from, LocalDate to) {
    Set<CessionKey> ceded = new HashSet<>(cessions.cededKeys(companyId));
    return policies.approvedTransactions(companyId, from, to).stream()
        .filter(t -> !ceded.contains(new CessionKey(t.ref().policyId(), t.endorsementNo())))
        .toList();
  }

  private AllocationPreviewRow previewRow(PremiumTransaction txn) {
    try {
      CessionPlan plan = planner.plan(txn);
      return AllocationPreviewRow.of(txn, plan, null);
    } catch (BusinessRuleException ex) {
      return AllocationPreviewRow.of(txn, null, ex.getMessage());
    }
  }

  /** Counts and messages of a run. */
  private static final class RunTally {
    private int ceded;
    private int failed;
    private BigDecimal premium = Money.zero();
    private final List<String> messages = new ArrayList<>();

    void success(Cession c) {
      ceded++;
      BigDecimal treatyAndFac =
          c.premiumOf(RiLayer.QUOTA_SHARE)
              .add(c.premiumOf(RiLayer.SURPLUS))
              .add(c.premiumOf(RiLayer.FAC));
      premium = premium.add(c.toBase(treatyAndFac));
    }

    void failure(String message) {
      failed++;
      if (messages.size() < MAX_MESSAGES) {
        messages.add(message);
      }
    }

    JobOutcome outcome() {
      return new JobOutcome(
          ceded, ceded + " transaction(s) ceded, " + failed + " failed, premium ceded " + premium);
    }
  }
}
