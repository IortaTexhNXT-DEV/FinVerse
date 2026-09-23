package com.iortatechnxt.finverse.budget.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.budget.api.dto.CreateBudgetRequest;
import com.iortatechnxt.finverse.budget.domain.Budget;
import com.iortatechnxt.finverse.budget.domain.BudgetHeader;
import com.iortatechnxt.finverse.budget.domain.BudgetLine;
import com.iortatechnxt.finverse.budget.domain.BudgetRepository;
import com.iortatechnxt.finverse.budget.domain.BudgetStatus;
import com.iortatechnxt.finverse.budget.domain.BudgetVersionType;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.organization.domain.Company;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.period.service.PeriodService;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Budget versions: creation (original or revision), submission and maker-checker approval.
 *
 * <p>Approving a revision supersedes the previously approved version, so exactly one approved
 * version per company and fiscal year drives budget monitoring.
 */
@Service
@Transactional
public class BudgetService {

  static final String ENTITY = "Budget";
  private static final Set<BudgetStatus> DEAD = EnumSet.of(BudgetStatus.REJECTED);

  private final BudgetRepository budgets;
  private final OrganizationService organization;
  private final PeriodService periods;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param budgets budget repository
   * @param organization organization service
   * @param periods period service
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public BudgetService(
      BudgetRepository budgets,
      OrganizationService organization,
      PeriodService periods,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.budgets = budgets;
    this.organization = organization;
    this.periods = periods;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists budget versions.
   *
   * @param companyId company
   * @param fiscalYear year, or null for all years
   * @return versions, newest first
   */
  @Transactional(readOnly = true)
  public List<Budget> list(Long companyId, Integer fiscalYear) {
    List<Budget> list =
        fiscalYear == null
            ? budgets.findByCompanyIdOrderByFiscalYearDescVersionNoDesc(companyId)
            : budgets.findByCompanyIdAndFiscalYearOrderByVersionNoDesc(companyId, fiscalYear);
    list.forEach(BudgetService::initialize);
    return list;
  }

  /**
   * Gets a version with its lines.
   *
   * @param id id
   * @return budget
   */
  @Transactional(readOnly = true)
  public Budget get(Long id) {
    Budget budget =
        budgets.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    initialize(budget);
    return budget;
  }

  /**
   * The approved version used for monitoring.
   *
   * @param companyId company
   * @param fiscalYear year
   * @return latest approved version if any
   */
  @Transactional(readOnly = true)
  public Optional<Budget> approved(Long companyId, int fiscalYear) {
    Optional<Budget> budget =
        budgets.findFirstByCompanyIdAndFiscalYearAndStatusInOrderByVersionNoDesc(
            companyId, fiscalYear, EnumSet.of(BudgetStatus.APPROVED));
    budget.ifPresent(BudgetService::initialize);
    return budget;
  }

  /**
   * Creates a draft version. A REVISED version copies the lines of the latest approved version (or
   * of {@code copyFromId}); an ORIGINAL version may copy from any version.
   *
   * @param request request
   * @return draft
   */
  public Budget create(CreateBudgetRequest request) {
    Company company = organization.requireActiveCompany(request.companyId());
    requireFiscalYear(request.companyId(), request.fiscalYear());
    Optional<Budget> source = source(request);
    if (request.versionType() == BudgetVersionType.ORIGINAL
        && budgets.countByCompanyIdAndFiscalYearAndVersionTypeAndStatusNotIn(
                request.companyId(), request.fiscalYear(), BudgetVersionType.ORIGINAL, DEAD)
            > 0) {
      throw new BusinessRuleException(
          "DUPLICATE_BUDGET",
          "An original budget for " + request.fiscalYear() + " already exists; create a revision");
    }
    if (request.versionType() == BudgetVersionType.REVISED && source.isEmpty()) {
      throw new BusinessRuleException(
          "NO_APPROVED_BUDGET", "A revision needs an approved budget to start from");
    }
    int versionNo = budgets.maxVersion(request.companyId(), request.fiscalYear()) + 1;
    Budget budget =
        new Budget(
            new BudgetHeader(
                request.companyId(),
                request.fiscalYear(),
                versionNo,
                request.versionType(),
                request.name().trim(),
                company.getBaseCurrency(),
                source.map(Budget::getId).orElse(null)));
    source.ifPresent(
        s -> budget.replaceLines(s.getLines().stream().map(BudgetLine::values).toList()));
    Budget saved = budgets.save(budget);
    audit.record(
        ENTITY,
        saved.getId(),
        AuditAction.CREATE,
        "Created "
            + request.versionType()
            + " budget v"
            + versionNo
            + " FY "
            + request.fiscalYear());
    return saved;
  }

  /**
   * Submits a version for approval.
   *
   * @param id id
   * @return budget
   */
  public Budget submit(Long id) {
    Budget budget = get(id);
    budget.submit(currentUser.username(), clock.instant());
    audit.record(ENTITY, id, AuditAction.SUBMIT, "Submitted budget v" + budget.getVersionNo());
    return budget;
  }

  /**
   * Approves a version; any previously approved version of the year is superseded.
   *
   * @param id id
   * @return budget
   */
  public Budget approve(Long id) {
    Budget budget = get(id);
    budget.approve(currentUser.username(), clock.instant());
    budgets
        .findByCompanyIdAndFiscalYearOrderByVersionNoDesc(
            budget.getCompanyId(), budget.getFiscalYear())
        .stream()
        .filter(b -> !b.getId().equals(budget.getId()))
        .forEach(Budget::supersede);
    audit.record(ENTITY, id, AuditAction.AUTHORIZE, "Approved budget v" + budget.getVersionNo());
    return budget;
  }

  /**
   * Rejects a submitted version.
   *
   * @param id id
   * @param reason reason
   * @return budget
   */
  public Budget reject(Long id, String reason) {
    Budget budget = get(id);
    budget.reject(reason);
    audit.record(ENTITY, id, AuditAction.REJECT, "Rejected budget: " + reason);
    return budget;
  }

  /** Loads lines and monthly amounts so callers can use the budget outside the transaction. */
  private static void initialize(Budget budget) {
    budget.getLines().forEach(BudgetLine::annual);
  }

  private Optional<Budget> source(CreateBudgetRequest request) {
    if (request.copyFromId() != null) {
      Budget from = get(request.copyFromId());
      if (!from.getCompanyId().equals(request.companyId())) {
        throw new BusinessRuleException(
            "BUDGET_OTHER_COMPANY", "Budget belongs to another company");
      }
      return Optional.of(from);
    }
    return request.versionType() == BudgetVersionType.REVISED
        ? approved(request.companyId(), request.fiscalYear())
        : Optional.empty();
  }

  private void requireFiscalYear(Long companyId, int fiscalYear) {
    boolean defined =
        periods.listYears(companyId).stream().anyMatch(y -> y.getYearCode() == fiscalYear);
    if (!defined) {
      throw new BusinessRuleException(
          "NO_FISCAL_YEAR", "Fiscal year " + fiscalYear + " is not defined for the company");
    }
  }
}
