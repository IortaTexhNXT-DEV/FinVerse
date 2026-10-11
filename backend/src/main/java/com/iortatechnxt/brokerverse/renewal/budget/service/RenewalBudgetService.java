package com.iortatechnxt.brokerverse.renewal.budget.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalBudget;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalBudgetHistory;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalBudgetHistoryRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalBudgetRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Annual Renewal Budget maintenance (BDOI Renewal FRS FRRN.042): create and update a budget record
 * of a fiscal year, segment and hierarchy with its monthly new, renewal and organic budget; the
 * validations of FRRN.042.04; the history of every amount changed (FRRN.042.06); and the budget of
 * a period for the dashboard (FRRN.002.02.01).
 */
@Service
@Transactional
public class RenewalBudgetService {

  /** Segment whose budgets are not set per Account Officer. */
  public static final String CBG = "CBG";

  /** Measure of the premium budget. */
  public static final String PREMIUM = "PREMIUM";

  /** Measure of the commission budget. */
  public static final String COMMISSION = "COMMISSION";

  private static final String ENTITY = "RenewalBudget";

  private final RenewalBudgetRepository budgets;
  private final RenewalBudgetHistoryRepository history;
  private final RenewalBudgetValidator validator;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param budgets budget records
   * @param history amount history
   * @param validator validations of a record
   * @param audit audit trail
   * @param currentUser user
   * @param clock clock
   */
  public RenewalBudgetService(
      RenewalBudgetRepository budgets,
      RenewalBudgetHistoryRepository history,
      RenewalBudgetValidator validator,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.budgets = budgets;
    this.history = history;
    this.validator = validator;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Creates the budget record of the key or updates it, keeping the history of every amount
   * changed.
   *
   * @param companyId company
   * @param input key, heads and the amounts of the months given
   * @return the record
   */
  public RenewalBudget save(Long companyId, BudgetInput input) {
    List<String> errors = validator.validate(input);
    if (!errors.isEmpty()) {
      throw new BusinessRuleException("RNW_BUDGET_INVALID", String.join("; ", errors));
    }
    RenewalBudget.Key key = normalised(input.key());
    boolean created = budgets.findByCompanyIdAndNaturalKey(companyId, key.text()).isEmpty();
    RenewalBudget budget =
        budgets
            .findByCompanyIdAndNaturalKey(companyId, key.text())
            .orElseGet(() -> budgets.save(new RenewalBudget(companyId, key)));
    budget.heads(input.heads());
    for (RenewalBudget.Month month : input.months()) {
      RenewalBudget.Month before = budget.month(month.monthNo());
      if (!created) {
        keep(budget, month, before);
      }
      budget.month(month);
    }
    RenewalBudget saved = budgets.save(budget);
    audit.record(
        ENTITY,
        saved.getId(),
        created ? AuditAction.CREATE : AuditAction.UPDATE,
        (created ? "Renewal budget created: " : "Renewal budget updated: ") + label(saved));
    return saved;
  }

  /**
   * The budget records of a fiscal year (Budget Inquiry, FRRN.042.05).
   *
   * @param companyId company
   * @param fiscalYear fiscal year
   * @return records
   */
  @Transactional(readOnly = true)
  public List<RenewalBudget> list(Long companyId, int fiscalYear) {
    return budgets.findByCompanyIdAndFiscalYearOrderBySegmentAscRegionAscTeamAscIdAsc(
        companyId, fiscalYear);
  }

  /**
   * A budget record.
   *
   * @param companyId company
   * @param id record
   * @return record
   */
  @Transactional(readOnly = true)
  public RenewalBudget get(Long companyId, Long id) {
    return budgets
        .findById(id)
        .filter(b -> b.getCompanyId().equals(companyId))
        .orElseThrow(() -> new ResourceNotFoundException("Renewal budget", String.valueOf(id)));
  }

  /**
   * The history of a budget record, latest first (FRRN.042.06).
   *
   * @param companyId company
   * @param id record
   * @return changes
   */
  @Transactional(readOnly = true)
  public List<RenewalBudgetHistory> history(Long companyId, Long id) {
    get(companyId, id);
    return history.findByBudgetIdOrderByIdDesc(id);
  }

  /**
   * The renewal budget of a period for the dashboard: the renewal amounts of the months that fall
   * in the period, of the records of the segment and Account Officer when given.
   *
   * @param companyId company
   * @param query measure, period, segment and officer
   * @return budget
   */
  @Transactional(readOnly = true)
  public BigDecimal budgetOf(Long companyId, BudgetQuery query) {
    YearMonth first = YearMonth.from(query.from());
    YearMonth last = YearMonth.from(query.to());
    BigDecimal total = BigDecimal.ZERO;
    for (RenewalBudget b :
        budgets.findByCompanyIdAndFiscalYearBetween(companyId, first.getYear(), last.getYear())) {
      if (!counts(b, query)) {
        continue;
      }
      for (RenewalBudget.Month m : b.getMonths()) {
        YearMonth ym = YearMonth.of(b.getFiscalYear(), m.monthNo());
        if (!ym.isBefore(first) && !ym.isAfter(last)) {
          total = total.add(query.type().apply(m));
        }
      }
    }
    return total;
  }

  private static boolean counts(RenewalBudget b, BudgetQuery query) {
    boolean segment = query.segment() == null || query.segment().strip().equals(b.getSegment());
    boolean officer =
        query.officer() == null || query.officer().strip().equals(b.getAccountOfficer());
    return b.getMeasure().equals(query.measure()) && segment && officer;
  }

  private void keep(RenewalBudget budget, RenewalBudget.Month after, RenewalBudget.Month before) {
    String month = monthName(after.monthNo());
    change(budget, "New budget - " + month, before.newAmount(), after.newAmount());
    change(budget, "Renewal budget - " + month, before.renewalAmount(), after.renewalAmount());
    change(budget, "Organic budget - " + month, before.organicAmount(), after.organicAmount());
  }

  private void change(RenewalBudget budget, String field, BigDecimal before, BigDecimal after) {
    if (before.compareTo(after) != 0) {
      history.save(
          new RenewalBudgetHistory(
              budget.getId(),
              field,
              new BigDecimal[] {before, after},
              currentUser.username(),
              clock.instant()));
    }
  }

  private RenewalBudget.Key normalised(RenewalBudget.Key key) {
    boolean cbg = CBG.equals(key.segment() == null ? null : key.segment().strip());
    return new RenewalBudget.Key(
        key.fiscalYear(),
        key.measure(),
        key.segment().strip(),
        validator.corporate(key.segment()) ? null : strip(key.region()),
        strip(key.team()),
        strip(key.subTeam()),
        cbg ? null : strip(key.accountOfficer()));
  }

  private static String label(RenewalBudget b) {
    return b.getFiscalYear()
        + " "
        + b.getSegment()
        + (b.getRegion() == null ? "" : " " + b.getRegion())
        + (b.getTeam() == null ? "" : " " + b.getTeam());
  }

  /**
   * The name of a month.
   *
   * @param monthNo 1 to 12
   * @return e.g. March
   */
  public static String monthName(int monthNo) {
    return java.time.Month.of(monthNo).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
  }

  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }

  private static String strip(String s) {
    return blank(s) ? null : s.strip();
  }

  /**
   * A budget record to save.
   *
   * @param key fiscal year, measure, segment and hierarchy
   * @param heads heads of the hierarchy
   * @param months the months given (others unchanged)
   */
  public record BudgetInput(
      RenewalBudget.Key key, RenewalBudget.Heads heads, List<RenewalBudget.Month> months) {

    /** Defensive copy. */
    public BudgetInput {
      months = months == null ? List.of() : List.copyOf(months);
    }
  }

  /**
   * The budget asked by the dashboard.
   *
   * @param measure PREMIUM or COMMISSION
   * @param from first day of the period
   * @param to last day of the period
   * @param segment segment or null for all
   * @param officer Account Officer or null for all
   * @param type the amount of a month that counts (renewal, new or organic)
   */
  public record BudgetQuery(
      String measure,
      LocalDate from,
      LocalDate to,
      String segment,
      String officer,
      Function<RenewalBudget.Month, BigDecimal> type) {}
}
