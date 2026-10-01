package com.iortatechnxt.brokerverse.security.api.dto;

import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import com.iortatechnxt.brokerverse.common.security.UserDataScope.CompanyScope;
import com.iortatechnxt.brokerverse.security.service.OrganizationUnits.BranchUnit;
import com.iortatechnxt.brokerverse.security.service.OrganizationUnits.CompanyUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The data scope of a user with the names of its companies and branches (Data access section).
 *
 * @param allCompanies every company and branch
 * @param companies granted companies (empty when all companies)
 * @param description the scope in business words
 */
public record DataScopeResponse(
    boolean allCompanies, List<CompanyAccess> companies, String description) {

  /**
   * Builds the response.
   *
   * @param scope scope
   * @param units known companies and branches
   * @param description scope in words
   * @return response
   */
  public static DataScopeResponse of(
      UserDataScope scope, List<CompanyUnit> units, String description) {
    Map<Long, CompanyUnit> byId =
        units.stream().collect(Collectors.toMap(CompanyUnit::id, Function.identity(), (a, b) -> a));
    return new DataScopeResponse(
        scope.allCompanies(),
        scope.companies().stream().map(c -> CompanyAccess.of(c, byId.get(c.companyId()))).toList(),
        description);
  }

  /**
   * One granted company.
   *
   * @param companyId company id
   * @param code company code (null when unknown)
   * @param name company name (null when unknown)
   * @param allBranches every branch of the company
   * @param branches granted branches (empty when all branches)
   */
  public record CompanyAccess(
      Long companyId, String code, String name, boolean allBranches, List<BranchAccess> branches) {

    static CompanyAccess of(CompanyScope c, CompanyUnit unit) {
      Map<Long, BranchUnit> known =
          unit == null
              ? Map.of()
              : unit.branches().stream()
                  .collect(Collectors.toMap(BranchUnit::id, Function.identity(), (a, b) -> a));
      return new CompanyAccess(
          c.companyId(),
          unit == null ? null : unit.code(),
          unit == null ? null : unit.name(),
          c.allBranches(),
          c.branchIds().stream()
              .map(
                  id -> {
                    BranchUnit b = known.get(id);
                    return new BranchAccess(
                        id, b == null ? null : b.code(), b == null ? null : b.name());
                  })
              .toList());
    }
  }

  /**
   * One granted branch.
   *
   * @param branchId branch id
   * @param code branch code (null when unknown)
   * @param name branch name (null when unknown)
   */
  public record BranchAccess(Long branchId, String code, String name) {}
}
