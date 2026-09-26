package com.iortatechnxt.brokerverse.closing.service;

import com.iortatechnxt.brokerverse.closing.domain.FxRevaluationRun;
import com.iortatechnxt.brokerverse.closing.domain.FxRevaluationRunRepository;
import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.journal.domain.JournalStatus;
import com.iortatechnxt.brokerverse.ledger.service.AccountBalance;
import com.iortatechnxt.brokerverse.ledger.service.BalanceQuery;
import com.iortatechnxt.brokerverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.period.domain.AccountingPeriod;
import com.iortatechnxt.brokerverse.period.domain.FiscalYear;
import com.iortatechnxt.brokerverse.period.domain.FiscalYearStatus;
import com.iortatechnxt.brokerverse.period.domain.PeriodStatus;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Automatic period-end and year-end checklists (spec 15.3 / 21.2): each control is a system check
 * with a pass/fail result; closing is allowed only when every blocking control passes.
 */
@Service
@Transactional(readOnly = true)
public class ClosingChecklistService {

  private static final Set<JournalStatus> OPEN_JOURNALS =
      EnumSet.of(JournalStatus.DRAFT, JournalStatus.PENDING_APPROVAL, JournalStatus.REJECTED);
  private static final Set<PeriodStatus> DONE =
      EnumSet.of(PeriodStatus.CLOSED, PeriodStatus.CLOSING);
  private static final Set<PeriodStatus> RECEIVES_CLOSING =
      EnumSet.of(PeriodStatus.CLOSING, PeriodStatus.REOPENED);
  private static final String RECONCILIATIONS = "RECONCILIATIONS";
  private static final String RECONCILIATIONS_LABEL = "Reconciliations completed";

  private final PeriodService periods;
  private final JournalBatchRepository journals;
  private final LedgerQueryService ledger;
  private final FxRevaluationRunRepository revaluations;
  private final FxRevaluationCalculator calculator;
  private final ChartOfAccountsService accounts;
  private final OrganizationService organization;
  private final ObjectProvider<ReconciliationStatusProvider> reconciliations;
  private final ObjectProvider<PeriodEndCheckProvider> moduleChecks;

  /**
   * Creates the service.
   *
   * @param periods period service
   * @param journals journal repository (pending journal counts)
   * @param ledger ledger read model
   * @param revaluations revaluation runs
   * @param calculator revaluation calculator
   * @param accounts chart of accounts
   * @param organization organization service
   * @param reconciliations reconciliation status providers (optional)
   * @param moduleChecks period-end controls of other modules (optional)
   */
  public ClosingChecklistService(
      PeriodService periods,
      JournalBatchRepository journals,
      LedgerQueryService ledger,
      FxRevaluationRunRepository revaluations,
      FxRevaluationCalculator calculator,
      ChartOfAccountsService accounts,
      OrganizationService organization,
      ObjectProvider<ReconciliationStatusProvider> reconciliations,
      ObjectProvider<PeriodEndCheckProvider> moduleChecks) {
    this.periods = periods;
    this.journals = journals;
    this.ledger = ledger;
    this.revaluations = revaluations;
    this.calculator = calculator;
    this.accounts = accounts;
    this.organization = organization;
    this.reconciliations = reconciliations;
    this.moduleChecks = moduleChecks;
  }

  /**
   * Monthly close checklist of a period.
   *
   * @param companyId company
   * @param periodId period
   * @return controls
   */
  public List<CheckItem> periodEnd(Long companyId, Long periodId) {
    AccountingPeriod period = periods.getPeriod(periodId);
    requireCompany(companyId, period.getCompanyId());
    List<CheckItem> items = new ArrayList<>();
    items.add(
        CheckItem.of(
            "PERIOD_STATUS",
            "Period is open or in soft close",
            period.getStatus() == PeriodStatus.OPEN
                || RECEIVES_CLOSING.contains(period.getStatus()),
            "Period " + period.getName() + " is " + period.getStatus()));
    items.add(pendingJournals(companyId, period.getStartDate(), period.getEndDate()));
    items.add(unreconciled(companyId, period.getEndDate()));
    items.add(revaluation(companyId, period));
    items.add(trialBalance(companyId, period.getEndDate()));
    moduleChecks
        .orderedStream()
        .forEach(
            p ->
                items.addAll(
                    p.periodEndChecks(companyId, period.getStartDate(), period.getEndDate())));
    return items;
  }

  /**
   * Pre-close checklist of a fiscal year.
   *
   * @param companyId company
   * @param fiscalYearId year
   * @return controls
   */
  public List<CheckItem> yearEnd(Long companyId, Long fiscalYearId) {
    FiscalYear year = periods.getYear(fiscalYearId);
    requireCompany(companyId, year.getCompanyId());
    List<AccountingPeriod> list = periods.listPeriods(fiscalYearId);
    List<CheckItem> items = new ArrayList<>();
    items.add(
        CheckItem.of(
            "YEAR_OPEN",
            "Fiscal year not yet closed",
            year.getStatus() == FiscalYearStatus.OPEN,
            "Fiscal year " + year.getYearCode() + " is " + year.getStatus()));
    String notDone =
        list.stream()
            .filter(p -> !DONE.contains(p.getStatus()))
            .map(p -> p.getName() + " " + p.getStatus())
            .collect(Collectors.joining(", "));
    items.add(
        CheckItem.of(
            "PERIODS_CLOSED",
            "All periods CLOSED or CLOSING",
            notDone.isEmpty(),
            notDone.isEmpty() ? "All " + list.size() + " periods are closed or closing" : notDone));
    AccountingPeriod last = list.get(list.size() - 1);
    items.add(
        CheckItem.of(
            "FINAL_PERIOD_CLOSING",
            "Final period in soft close to receive the closing journal",
            RECEIVES_CLOSING.contains(last.getStatus()),
            "Period " + last.getName() + " is " + last.getStatus()));
    items.add(pendingJournals(companyId, year.getStartDate(), year.getEndDate()));
    items.add(trialBalance(companyId, year.getEndDate()));
    items.add(revaluation(companyId, last));
    items.add(unreconciled(companyId, year.getEndDate()));
    items.add(retainedEarnings(companyId));
    return items;
  }

  private CheckItem pendingJournals(Long companyId, LocalDate from, LocalDate to) {
    long open =
        journals.countByCompanyIdAndStatusInAndValueDateBetween(companyId, OPEN_JOURNALS, from, to);
    return CheckItem.of(
        "NO_PENDING_JOURNALS",
        "No journals in draft, rejected or awaiting authorization",
        open == 0,
        open + " journal(s) not yet posted or cancelled");
  }

  /**
   * Unreconciled items of every reconciliation provider up to a date. A warning, not a blocking
   * control: reconciling items such as deposits in transit and unpresented cheques are normal at a
   * period end and are carried in the reconciliation statement, so they are shown for review but do
   * not prevent the close.
   */
  private CheckItem unreconciled(Long companyId, LocalDate asOf) {
    List<ReconciliationStatusProvider> providers = reconciliations.orderedStream().toList();
    if (providers.isEmpty()) {
      return CheckItem.warning(
          RECONCILIATIONS,
          RECONCILIATIONS_LABEL,
          true,
          "No reconciliation module registered (0 unreconciled items)");
    }
    long count = 0;
    List<String> open = new ArrayList<>();
    for (ReconciliationStatusProvider p : providers) {
      long items = p.unreconciledItems(companyId, asOf);
      count += items;
      if (items > 0) {
        open.add(p.name() + " " + items);
      }
    }
    String detail =
        count == 0
            ? "No unreconciled items up to "
                + asOf
                + ": "
                + providers.stream()
                    .map(ReconciliationStatusProvider::name)
                    .collect(Collectors.joining(", "))
            : count
                + " unreconciled item(s) up to "
                + asOf
                + " ("
                + String.join(", ", open)
                + "); review them before closing";
    return CheckItem.warning(RECONCILIATIONS, RECONCILIATIONS_LABEL, count == 0, detail);
  }

  private CheckItem trialBalance(Long companyId, LocalDate asOf) {
    BigDecimal difference =
        ledger.balances(BalanceQuery.asOf(companyId, asOf)).stream()
            .map(AccountBalance::netBase)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return CheckItem.of(
        "TB_BALANCED",
        "Trial balance in balance",
        difference.signum() == 0,
        difference.signum() == 0 ? "Debits equal credits" : "Difference " + difference);
  }

  /**
   * FX revaluation status of a period.
   *
   * @param companyId company
   * @param period period
   * @return control
   */
  CheckItem revaluation(Long companyId, AccountingPeriod period) {
    Optional<FxRevaluationRun> run =
        revaluations.findByCompanyIdAndPeriodId(companyId, period.getId());
    if (run.isPresent()) {
      String batch = run.get().getJournalBatchNo();
      return CheckItem.of(
          "FX_REVALUATION",
          "Foreign currency balances revalued",
          true,
          batch == null ? "Revalued, nothing to post" : "Posted " + batch);
    }
    FxRevaluationCalculator.Result result = calculator.calculate(companyId, period.getEndDate());
    boolean required = result.needsPosting() || !result.missingRates().isEmpty();
    return CheckItem.of(
        "FX_REVALUATION",
        "Foreign currency balances revalued",
        !required,
        required
            ? "Not run: " + result.items().size() + " foreign currency balance(s) to revalue"
            : "Not required: no foreign currency differences");
  }

  private CheckItem retainedEarnings(Long companyId) {
    Company company = organization.getCompany(companyId);
    String code = company.getRetainedEarningsAccount();
    Optional<GlAccount> account =
        code == null
            ? Optional.empty()
            : accounts.list(companyId).stream().filter(a -> a.getCode().equals(code)).findFirst();
    boolean valid =
        account.isPresent()
            && account.get().isPostable()
            && account.get().getAccountClass() == AccountClass.EQUITY;
    return CheckItem.of(
        "RETAINED_EARNINGS",
        "Retained earnings account configured",
        valid,
        valid ? "Account " + code : "Company retained earnings account is missing or invalid");
  }

  private static void requireCompany(Long companyId, Long ownerId) {
    if (!ownerId.equals(companyId)) {
      throw new BusinessRuleException("PERIOD_OTHER_COMPANY", "Period belongs to another company");
    }
  }
}
