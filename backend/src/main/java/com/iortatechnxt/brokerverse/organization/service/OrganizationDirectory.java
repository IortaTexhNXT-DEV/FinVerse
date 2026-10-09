package com.iortatechnxt.brokerverse.organization.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cached, read-only view of the enterprise structure ({@link OrganizationCaches#UNITS}) for
 * look-ups on hot paths (codes and names on screens, reports, event envelopes). Use {@link
 * OrganizationService} when you need the entity or the maker-checker checks.
 */
@Service
@Transactional(readOnly = true)
public class OrganizationDirectory {

  private static final String COMPANY = "Company";

  private final CompanyRepository companies;
  private final BranchRepository branches;

  /**
   * Creates the directory.
   *
   * @param companies company repository
   * @param branches branch repository
   */
  public OrganizationDirectory(CompanyRepository companies, BranchRepository branches) {
    this.companies = companies;
    this.branches = branches;
  }

  /**
   * A company.
   *
   * @param id company id
   * @return company
   */
  @Cacheable(cacheNames = OrganizationCaches.UNITS, key = "'company:' + #id")
  public CompanyRef company(Long id) {
    return companies
        .findById(id)
        .map(CompanyRef::of)
        .orElseThrow(() -> new ResourceNotFoundException(COMPANY, id));
  }

  /**
   * Every company of the installation.
   *
   * @return companies by id
   */
  public List<CompanyRef> companies() {
    return companies.findAll().stream().map(CompanyRef::of).toList();
  }

  /**
   * A company by code.
   *
   * @param code company code
   * @return company
   */
  @Cacheable(cacheNames = OrganizationCaches.UNITS, key = "'company-code:' + #code")
  public CompanyRef companyByCode(String code) {
    return companies
        .findByCode(code)
        .map(CompanyRef::of)
        .orElseThrow(() -> new ResourceNotFoundException(COMPANY, code));
  }

  /**
   * The client profile of a company: the identity that documents, messages and labels use.
   *
   * @param id company id
   * @return profile
   */
  @Cacheable(cacheNames = OrganizationCaches.UNITS, key = "'profile:' + #id")
  public ClientProfile profile(Long id) {
    Company c =
        companies.findById(id).orElseThrow(() -> new ResourceNotFoundException(COMPANY, id));
    String headOffice = c.getHeadOfficeCode();
    if (headOffice == null) {
      headOffice =
          branches.findByCompanyIdOrderByCode(id).stream()
              .filter(Branch::isHeadOffice)
              .map(Branch::getCode)
              .findFirst()
              .orElse(null);
    }
    return new ClientProfile(
        c.getId(),
        c.getCode(),
        c.getName(),
        c.getShortName(),
        c.getGroupName(),
        c.getAddress(),
        c.getTaxId(),
        c.getLogoRef(),
        headOffice,
        c.getDefaultBankCode(),
        c.getBaseCurrency());
  }

  /**
   * A branch.
   *
   * @param id branch id
   * @return branch
   */
  @Cacheable(cacheNames = OrganizationCaches.UNITS, key = "'branch:' + #id")
  public BranchRef branch(Long id) {
    return branches
        .findById(id)
        .map(BranchRef::of)
        .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
  }

  /**
   * The branches of a company.
   *
   * @param companyId company id
   * @return branches ordered by code
   */
  @Cacheable(cacheNames = OrganizationCaches.UNITS, key = "'branches:' + #companyId")
  public List<BranchRef> branchesOf(Long companyId) {
    return branches.findByCompanyIdOrderByCode(companyId).stream().map(BranchRef::of).toList();
  }

  /**
   * A company (cached view).
   *
   * @param id id
   * @param code code
   * @param name name
   * @param baseCurrency base currency
   * @param active authorized and active
   */
  public record CompanyRef(Long id, String code, String name, String baseCurrency, boolean active) {

    static CompanyRef of(Company c) {
      return new CompanyRef(c.getId(), c.getCode(), c.getName(), c.getBaseCurrency(), c.isActive());
    }
  }

  /**
   * The client profile of a company (cached view).
   *
   * @param id company id
   * @param code company code
   * @param legalName legal name (letterheads, signatures of messages)
   * @param shortName short name in texts and labels
   * @param groupName group the company belongs to, null when none
   * @param address registered address
   * @param taxId tax identification number
   * @param logoRef logo reference of documents, null for the theme pack logo
   * @param headOfficeCode code of the head office in files and for records without a branch
   * @param defaultBankCode bank account code proposed by default, may be null
   * @param baseCurrency base currency
   */
  public record ClientProfile(
      Long id,
      String code,
      String legalName,
      String shortName,
      String groupName,
      String address,
      String taxId,
      String logoRef,
      String headOfficeCode,
      String defaultBankCode,
      String baseCurrency) {}

  /**
   * A branch (cached view).
   *
   * @param id id
   * @param companyId company id
   * @param code code
   * @param name name
   * @param headOffice head office
   * @param active authorized and active
   */
  public record BranchRef(
      Long id, Long companyId, String code, String name, boolean headOffice, boolean active) {

    static BranchRef of(Branch b) {
      return new BranchRef(
          b.getId(),
          b.getCompany().getId(),
          b.getCode(),
          b.getName(),
          b.isHeadOffice(),
          b.isActive());
    }
  }
}
