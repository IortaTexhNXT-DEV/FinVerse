package com.iortatechnxt.brokerverse.organization.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.organization.api.dto.BranchRequest;
import com.iortatechnxt.brokerverse.organization.api.dto.CompanyRequest;
import com.iortatechnxt.brokerverse.organization.api.dto.HolidayRequest;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.organization.domain.Holiday;
import com.iortatechnxt.brokerverse.organization.domain.HolidayRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains the enterprise structure: companies, branches and holiday calendars.
 *
 * <p>Companies and branches follow maker-checker: every change must be authorized by another user
 * before the record can be used for posting.
 */
@Service
@Transactional
public class OrganizationService {

  private static final String COMPANY = "Company";
  private static final String BRANCH = "Branch";

  private final CompanyRepository companies;
  private final BranchRepository branches;
  private final HolidayRepository holidays;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param companies company repository
   * @param branches branch repository
   * @param holidays holiday repository
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public OrganizationService(
      CompanyRepository companies,
      BranchRepository branches,
      HolidayRepository holidays,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.companies = companies;
    this.branches = branches;
    this.holidays = holidays;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists all companies.
   *
   * @return companies ordered by code
   */
  @Transactional(readOnly = true)
  public List<Company> listCompanies() {
    return companies.findAll(Sort.by("code"));
  }

  /**
   * Gets a company.
   *
   * @param id company id
   * @return company
   */
  @Transactional(readOnly = true)
  public Company getCompany(Long id) {
    return companies.findById(id).orElseThrow(() -> new ResourceNotFoundException(COMPANY, id));
  }

  /**
   * Returns an active company or fails.
   *
   * @param id company id
   * @return active company
   */
  @Transactional(readOnly = true)
  public Company requireActiveCompany(Long id) {
    Company company = getCompany(id);
    if (!company.isActive()) {
      throw new BusinessRuleException(
          "INACTIVE_COMPANY", "Company " + company.getCode() + " is not active");
    }
    return company;
  }

  /**
   * Creates a company (pending authorization).
   *
   * @param request request
   * @return created company
   */
  public Company createCompany(CompanyRequest request) {
    if (companies.existsByCode(request.code())) {
      throw new DuplicateResourceException(COMPANY, request.code());
    }
    Company company = new Company(request.code(), request.name(), request.baseCurrency());
    applyCompany(company, request);
    Company saved = companies.save(company);
    audit.record(
        COMPANY, saved.getCode(), AuditAction.CREATE, "Created company " + saved.getName());
    return saved;
  }

  /**
   * Updates a company; it returns to pending authorization.
   *
   * @param id company id
   * @param request request
   * @return updated company
   */
  public Company updateCompany(Long id, CompanyRequest request) {
    Company company = getCompany(id);
    company.setName(request.name());
    applyCompany(company, request);
    company.markModified();
    audit.record(
        COMPANY, company.getCode(), AuditAction.UPDATE, "Updated company " + company.getName());
    return company;
  }

  /**
   * Authorizes a pending company (checker).
   *
   * @param id company id
   * @return authorized company
   */
  public Company authorizeCompany(Long id) {
    Company company = getCompany(id);
    company.authorize(currentUser.username(), clock.instant());
    audit.record(COMPANY, company.getCode(), AuditAction.AUTHORIZE, "Authorized company");
    return company;
  }

  /**
   * Lists the branches of a company.
   *
   * @param companyId company id
   * @return branches
   */
  @Transactional(readOnly = true)
  public List<Branch> listBranches(Long companyId) {
    return branches.findByCompanyIdOrderByCode(companyId);
  }

  /**
   * Gets a branch.
   *
   * @param id branch id
   * @return branch
   */
  @Transactional(readOnly = true)
  public Branch getBranch(Long id) {
    return branches.findById(id).orElseThrow(() -> new ResourceNotFoundException(BRANCH, id));
  }

  /**
   * Returns an active branch or fails.
   *
   * @param id branch id
   * @return active branch
   */
  @Transactional(readOnly = true)
  public Branch requireActiveBranch(Long id) {
    Branch branch = getBranch(id);
    if (!branch.isActive()) {
      throw new BusinessRuleException(
          "INACTIVE_BRANCH", "Branch " + branch.getCode() + " is not active");
    }
    return branch;
  }

  /**
   * Creates a branch (pending authorization).
   *
   * @param request request
   * @return created branch
   */
  public Branch createBranch(BranchRequest request) {
    Company company = getCompany(request.companyId());
    if (branches.existsByCompanyIdAndCode(company.getId(), request.code())) {
      throw new DuplicateResourceException(BRANCH, request.code());
    }
    Branch branch = new Branch(company, request.code(), request.name(), request.openingDate());
    applyBranch(branch, request);
    Branch saved = branches.save(branch);
    audit.record(BRANCH, saved.getCode(), AuditAction.CREATE, "Created branch " + saved.getName());
    return saved;
  }

  /**
   * Updates a branch; it returns to pending authorization.
   *
   * @param id branch id
   * @param request request
   * @return updated branch
   */
  public Branch updateBranch(Long id, BranchRequest request) {
    Branch branch = getBranch(id);
    branch.setName(request.name());
    applyBranch(branch, request);
    branch.markModified();
    audit.record(
        BRANCH, branch.getCode(), AuditAction.UPDATE, "Updated branch " + branch.getName());
    return branch;
  }

  /**
   * Authorizes a pending branch (checker).
   *
   * @param id branch id
   * @return authorized branch
   */
  public Branch authorizeBranch(Long id) {
    Branch branch = getBranch(id);
    branch.authorize(currentUser.username(), clock.instant());
    audit.record(BRANCH, branch.getCode(), AuditAction.AUTHORIZE, "Authorized branch");
    return branch;
  }

  /**
   * Deactivates a branch (branches are never deleted).
   *
   * @param id branch id
   * @return deactivated branch
   */
  public Branch deactivateBranch(Long id) {
    Branch branch = getBranch(id);
    branch.deactivate();
    audit.record(BRANCH, branch.getCode(), AuditAction.DEACTIVATE, "Deactivated branch");
    return branch;
  }

  /**
   * Lists holidays of a company for a year.
   *
   * @param companyId company id
   * @param year calendar year
   * @return holidays
   */
  @Transactional(readOnly = true)
  public List<Holiday> listHolidays(Long companyId, int year) {
    return holidays.findByCompanyIdAndHolidayDateBetweenOrderByHolidayDate(
        companyId, Year.of(year).atDay(1), Year.of(year).atMonth(Month.DECEMBER).atEndOfMonth());
  }

  /**
   * Adds a holiday.
   *
   * @param request request
   * @return holiday
   */
  public Holiday addHoliday(HolidayRequest request) {
    getCompany(request.companyId());
    Holiday saved =
        holidays.save(
            new Holiday(
                request.companyId(),
                request.branchId(),
                request.holidayDate(),
                request.description()));
    audit.record("Holiday", saved.getId(), AuditAction.CREATE, "Holiday " + request.holidayDate());
    return saved;
  }

  /**
   * Checks whether a date is a working day for a branch (not a weekly or declared holiday).
   *
   * @param branch branch
   * @param date date
   * @return true when working day
   */
  @Transactional(readOnly = true)
  public boolean isWorkingDay(Branch branch, LocalDate date) {
    String weekly = branch.getWeeklyHolidays();
    String day = String.valueOf(date.getDayOfWeek().getValue());
    boolean weeklyHoliday = weekly != null && List.of(weekly.split(",")).contains(day);
    return !weeklyHoliday && !holidays.isHoliday(branch.getCompany().getId(), branch.getId(), date);
  }

  private static void applyCompany(Company company, CompanyRequest request) {
    company.setTaxId(request.taxId());
    company.setAddress(request.address());
    company.setFiscalYearStartMonth(request.fiscalYearStartMonth());
    company.setBackValueDays(request.backValueDays());
    company.setForwardValueDays(request.forwardValueDays());
    company.setRetainedEarningsAccount(request.retainedEarningsAccount());
  }

  private static void applyBranch(Branch branch, BranchRequest request) {
    branch.setRegion(request.region());
    branch.setAddress(request.address());
    branch.setHeadOffice(request.headOffice());
    branch.setForexAuthorized(request.forexAuthorized());
    branch.setContactPhone(request.contactPhone());
    branch.setContactEmail(request.contactEmail());
    branch.setManagerName(request.managerName());
    branch.setWeeklyHolidays(request.weeklyHolidays());
  }
}
