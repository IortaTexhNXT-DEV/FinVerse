package com.iortatechnxt.brokerverse.common.security;

import java.util.List;
import java.util.function.Function;

/**
 * The company and branch data scope of the signed-in user (docs/architecture/DATA_SCOPE_DESIGN.md).
 * Fail closed: an unknown user, or a company or branch outside the scope, is refused with {@link
 * com.iortatechnxt.brokerverse.common.exception.DataScopeDeniedException} (HTTP 403, {@code
 * DATA_SCOPE_DENIED}). Background processing and the system-to-system APIs run as the system and
 * see every company. Implemented by the security module.
 */
public interface DataScope {

  /**
   * Refuses a company outside the scope. A null company (an absent optional filter) is not checked.
   *
   * @param companyId company id, may be null
   */
  void requireCompany(Long companyId);

  /**
   * Refuses a branch outside the scope. A null branch is checked as {@link #requireCompany}; with a
   * null company the company of the branch is looked up.
   *
   * @param companyId company id, may be null
   * @param branchId branch id, may be null
   */
  void requireBranch(Long companyId, Long branchId);

  /**
   * The allowed companies and branches, for list queries.
   *
   * @return scope ({@link UserDataScope#ALL} for the system)
   */
  UserDataScope allowed();

  /**
   * Keeps the rows of the allowed companies; rows without a company are kept.
   *
   * @param rows rows
   * @param companyOf company of a row, may answer null
   * @param <T> row type
   * @return allowed rows, in their order
   */
  default <T> List<T> filter(List<T> rows, Function<T, Long> companyOf) {
    UserDataScope scope = allowed();
    if (scope.allCompanies()) {
      return rows;
    }
    return rows.stream()
        .filter(
            r -> {
              Long company = companyOf.apply(r);
              return company == null || scope.allowsCompany(company);
            })
        .toList();
  }
}
