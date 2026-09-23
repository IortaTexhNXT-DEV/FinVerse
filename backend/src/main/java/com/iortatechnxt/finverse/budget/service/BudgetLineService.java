package com.iortatechnxt.finverse.budget.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.budget.api.dto.BudgetLineRequest;
import com.iortatechnxt.finverse.budget.domain.Budget;
import com.iortatechnxt.finverse.budget.domain.BudgetLine;
import com.iortatechnxt.finverse.budget.domain.BudgetLineValues;
import com.iortatechnxt.finverse.budget.service.BudgetActualsQuery.MonthlyActual;
import com.iortatechnxt.finverse.coa.domain.AccountClass;
import com.iortatechnxt.finverse.coa.domain.GlAccount;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.dimension.domain.DimensionType;
import com.iortatechnxt.finverse.dimension.service.DimensionService;
import com.iortatechnxt.finverse.period.domain.FiscalYear;
import com.iortatechnxt.finverse.period.service.PeriodService;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains the lines of a draft budget: grid save, CSV import and copy from prior-year actuals.
 *
 * <p>Lines are restricted to postable INCOME and EXPENSE accounts (the accounts compared in Budget
 * vs Actual); cost centres must be active dimension values.
 */
@Service
@Transactional
public class BudgetLineService {

  private final BudgetService budgets;
  private final ChartOfAccountsService accounts;
  private final DimensionService dimensions;
  private final PeriodService periods;
  private final BudgetActualsQuery actuals;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param budgets budget service
   * @param accounts chart of accounts
   * @param dimensions dimension service
   * @param periods period service
   * @param actuals ledger actuals query
   * @param audit audit trail
   */
  public BudgetLineService(
      BudgetService budgets,
      ChartOfAccountsService accounts,
      DimensionService dimensions,
      PeriodService periods,
      BudgetActualsQuery actuals,
      AuditTrailService audit) {
    this.budgets = budgets;
    this.accounts = accounts;
    this.dimensions = dimensions;
    this.periods = periods;
    this.actuals = actuals;
    this.audit = audit;
  }

  /**
   * Replaces the lines of a draft (grid save).
   *
   * @param id budget
   * @param lines lines
   * @return budget
   */
  public Budget saveLines(Long id, List<BudgetLineRequest> lines) {
    Budget budget = budgets.get(id);
    List<BudgetLineValues> values =
        validate(
            budget.getCompanyId(),
            lines.stream()
                .map(l -> new Candidate(l.accountCode(), l.costCenter(), l.months()))
                .toList());
    budget.replaceLines(values);
    audit.record(
        BudgetService.ENTITY, id, AuditAction.UPDATE, "Saved " + values.size() + " budget lines");
    return budget;
  }

  /**
   * Replaces the lines of a draft from a CSV file.
   *
   * @param id budget
   * @param csv file content
   * @return budget
   */
  public Budget importCsv(Long id, String csv) {
    Budget budget = budgets.get(id);
    List<BudgetLineValues> values =
        validate(
            budget.getCompanyId(),
            BudgetCsvParser.parse(csv).stream()
                .map(l -> new Candidate(l.accountCode(), l.costCenter(), l.months()))
                .toList());
    budget.replaceLines(values);
    audit.record(
        BudgetService.ENTITY,
        id,
        AuditAction.UPDATE,
        "Imported " + values.size() + " budget lines from CSV");
    return budget;
  }

  /**
   * Replaces the lines of a draft with the income and expense actuals of a prior fiscal year per
   * account, cost centre and month, adjusted by a percentage.
   *
   * @param id budget
   * @param sourceYear fiscal year whose actuals are copied
   * @param percent adjustment in percent
   * @return budget
   */
  public Budget copyFromActuals(Long id, int sourceYear, BigDecimal percent) {
    Budget budget = budgets.get(id);
    Long companyId = budget.getCompanyId();
    FiscalYear year =
        periods.listYears(companyId).stream()
            .filter(y -> y.getYearCode() == sourceYear)
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "NO_FISCAL_YEAR", "Fiscal year " + sourceYear + " is not defined"));
    Map<Long, GlAccount> byId =
        accounts.list(companyId).stream()
            .collect(Collectors.toMap(GlAccount::getId, Function.identity()));
    Map<String, BigDecimal[]> grid = new LinkedHashMap<>();
    Map<String, Candidate> keys = new LinkedHashMap<>();
    for (MonthlyActual a : actuals.monthly(companyId, year.getStartDate(), year.getEndDate())) {
      GlAccount account = byId.get(a.accountId());
      if (!isProfitAndLoss(account)) {
        continue;
      }
      String key = account.getCode() + "|" + a.costCenter();
      keys.putIfAbsent(key, new Candidate(account.getCode(), a.costCenter(), List.of()));
      int month = (int) ChronoUnit.MONTHS.between(year.getStartDate(), a.monthStart());
      BigDecimal natural =
          account.getAccountClass() == AccountClass.INCOME ? a.netDebit().negate() : a.netDebit();
      BigDecimal[] row = grid.computeIfAbsent(key, k -> zeros());
      row[month] = row[month].add(natural);
    }
    if (keys.isEmpty()) {
      throw new BusinessRuleException(
          "NO_ACTUALS", "No income or expense actuals were posted in fiscal year " + sourceYear);
    }
    List<Candidate> candidates = new ArrayList<>();
    keys.forEach(
        (key, c) ->
            candidates.add(
                new Candidate(
                    c.accountCode(),
                    c.costCenter(),
                    BudgetSpread.scale(List.of(grid.get(key)), percent))));
    List<BudgetLineValues> values = validate(companyId, candidates);
    budget.replaceLines(values);
    audit.record(
        BudgetService.ENTITY,
        id,
        AuditAction.UPDATE,
        "Copied FY " + sourceYear + " actuals " + percent.toPlainString() + "% into budget");
    return budget;
  }

  private List<BudgetLineValues> validate(Long companyId, List<Candidate> candidates) {
    Map<String, GlAccount> byCode =
        accounts.list(companyId).stream()
            .collect(Collectors.toMap(GlAccount::getCode, Function.identity()));
    Set<String> seen = new HashSet<>();
    List<String> errors = new ArrayList<>();
    List<BudgetLineValues> values = new ArrayList<>();
    for (Candidate c : candidates) {
      String costCenter =
          c.costCenter() == null || c.costCenter().isBlank() ? null : c.costCenter();
      GlAccount account = byCode.get(c.accountCode());
      if (!isBudgetable(account)) {
        errors.add("Account " + c.accountCode() + " is not a postable income or expense account");
        continue;
      }
      if (!seen.add(c.accountCode() + "|" + costCenter)) {
        errors.add("Account " + c.accountCode() + " / " + costCenter + " appears twice");
        continue;
      }
      dimensions.validateOptional(companyId, DimensionType.COST_CENTER, costCenter);
      values.add(new BudgetLineValues(account.getId(), account.getCode(), costCenter, c.months()));
    }
    if (!errors.isEmpty()) {
      throw new BusinessRuleException("BUDGET_LINES_INVALID", String.join("; ", errors));
    }
    return values;
  }

  private static boolean isBudgetable(GlAccount account) {
    return account != null && account.isPostable() && isProfitAndLoss(account);
  }

  private static boolean isProfitAndLoss(GlAccount account) {
    return account != null && !account.getAccountClass().isBalanceSheet();
  }

  private static BigDecimal[] zeros() {
    BigDecimal[] months = new BigDecimal[BudgetLine.MONTHS];
    Arrays.fill(months, Money.zero());
    return months;
  }

  private record Candidate(String accountCode, String costCenter, List<BigDecimal> months) {}
}
