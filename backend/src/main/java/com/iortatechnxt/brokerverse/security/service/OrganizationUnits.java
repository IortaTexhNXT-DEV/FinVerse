package com.iortatechnxt.brokerverse.security.service;

import java.util.List;
import java.util.Optional;

/**
 * Port: the companies and their branches, for the data scope
 * (docs/architecture/DATA_SCOPE_DESIGN.md) - which units a scope may grant, the names shown on the
 * Data access section and the company of a branch named without its company. Implemented by {@code
 * organization}; {@code security} never depends on it. Without an implementation no unit is known,
 * so only "All companies" can be granted and a branch without its company is refused.
 */
public interface OrganizationUnits {

  /**
   * Every company with its branches, ordered by code.
   *
   * @return companies
   */
  List<CompanyUnit> companies();

  /**
   * The company of a branch.
   *
   * @param branchId branch id
   * @return company id, empty for an unknown branch
   */
  Optional<Long> companyOfBranch(Long branchId);

  /**
   * One company.
   *
   * @param id company id
   * @param code company code
   * @param name company name
   * @param branches its branches, ordered by code
   */
  record CompanyUnit(Long id, String code, String name, List<BranchUnit> branches) {

    /** Defensive copy. */
    public CompanyUnit {
      branches = branches == null ? List.of() : List.copyOf(branches);
    }
  }

  /**
   * One branch.
   *
   * @param id branch id
   * @param code branch code
   * @param name branch name
   */
  record BranchUnit(Long id, String code, String name) {}
}
