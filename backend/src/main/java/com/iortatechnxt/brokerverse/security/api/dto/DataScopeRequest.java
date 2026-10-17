package com.iortatechnxt.brokerverse.security.api.dto;

import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import com.iortatechnxt.brokerverse.common.security.UserDataScope.CompanyScope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * The data scope of a user as entered on the Data access section (DATA_SCOPE_DESIGN.md).
 *
 * @param allCompanies every company and branch (the default)
 * @param companies granted companies when not all companies
 */
public record DataScopeRequest(
    boolean allCompanies, @Size(max = 500) List<@Valid @NotNull CompanyGrant> companies) {

  /**
   * The scope.
   *
   * @return scope value
   */
  public UserDataScope toScope() {
    if (allCompanies) {
      return UserDataScope.ALL;
    }
    return UserDataScope.of(
        companies == null ? List.of() : companies.stream().map(CompanyGrant::toScope).toList());
  }

  /**
   * One granted company.
   *
   * @param companyId company
   * @param allBranches every branch of the company
   * @param branchIds granted branches when not all branches
   */
  public record CompanyGrant(
      @NotNull Long companyId, boolean allBranches, @Size(max = 500) List<Long> branchIds) {

    CompanyScope toScope() {
      return allBranches
          ? CompanyScope.allBranchesOf(companyId)
          : CompanyScope.branchesOf(companyId, branchIds == null ? List.of() : branchIds);
    }
  }
}
