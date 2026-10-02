package com.iortatechnxt.brokerverse.organization.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.security.service.OrganizationUnits;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The companies and branches of the data scope (port {@link OrganizationUnits},
 * DATA_SCOPE_DESIGN.md): every company, whatever the scope of the reader, because the security
 * module decides what the reader may see. Branches and the company of a branch come from the cached
 * {@link OrganizationDirectory}.
 */
@Component
public class OrganizationUnitsAdapter implements OrganizationUnits {

  private final CompanyRepository companies;
  private final OrganizationDirectory directory;

  /**
   * Creates the adapter.
   *
   * @param companies companies
   * @param directory cached organization units
   */
  public OrganizationUnitsAdapter(CompanyRepository companies, OrganizationDirectory directory) {
    this.companies = companies;
    this.directory = directory;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CompanyUnit> companies() {
    return companies.findAll(Sort.by("code")).stream()
        .map(
            c ->
                new CompanyUnit(
                    c.getId(),
                    c.getCode(),
                    c.getName(),
                    directory.branchesOf(c.getId()).stream()
                        .map(b -> new BranchUnit(b.id(), b.code(), b.name()))
                        .toList()))
        .toList();
  }

  @Override
  public Optional<Long> companyOfBranch(Long branchId) {
    try {
      return Optional.of(directory.branch(branchId).companyId());
    } catch (ResourceNotFoundException ex) {
      return Optional.empty();
    }
  }
}
