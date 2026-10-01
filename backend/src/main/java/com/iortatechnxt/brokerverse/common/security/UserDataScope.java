package com.iortatechnxt.brokerverse.common.security;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The companies and branches a user may act for (docs/architecture/DATA_SCOPE_DESIGN.md): all
 * companies, or a list of companies, each with all its branches or a list of branches. Immutable; a
 * cached value. Its text form ({@link #text()}) is {@value #ALL_TEXT}, empty for a scope that
 * grants nothing, or {@code 12:*;14:3,5} (company 12 with all branches, company 14 with branches 3
 * and 5).
 *
 * @param allCompanies true for every company, present and future
 * @param companies granted companies (empty when {@code allCompanies}), ordered by company id
 */
public record UserDataScope(boolean allCompanies, List<CompanyScope> companies) {

  /** Text form of "all companies". */
  public static final String ALL_TEXT = "ALL";

  /** Every company and branch: the default of every user. */
  public static final UserDataScope ALL = new UserDataScope(true, List.of());

  /** Nothing: the scope of an unknown user. */
  public static final UserDataScope NONE = new UserDataScope(false, List.of());

  private static final String COMPANY_SEPARATOR = ";";
  private static final String BRANCH_SEPARATOR = ",";
  private static final String ALL_BRANCHES = "*";

  /** Normalises: one entry per company, ordered; no companies when all are granted. */
  public UserDataScope {
    if (allCompanies || companies == null) {
      companies = List.of();
    } else {
      Map<Long, CompanyScope> byCompany = new TreeMap<>();
      companies.forEach(c -> byCompany.merge(c.companyId(), c, CompanyScope::merge));
      companies = List.copyOf(byCompany.values());
    }
  }

  /**
   * A scope of listed companies.
   *
   * @param companies granted companies
   * @return scope
   */
  public static UserDataScope of(List<CompanyScope> companies) {
    return new UserDataScope(false, companies);
  }

  /**
   * Whether the company is in the scope.
   *
   * @param companyId company id
   * @return true when allowed
   */
  public boolean allowsCompany(Long companyId) {
    return allCompanies || company(companyId).isPresent();
  }

  /**
   * Whether the branch of the company is in the scope.
   *
   * @param companyId company id
   * @param branchId branch id
   * @return true when allowed
   */
  public boolean allowsBranch(Long companyId, Long branchId) {
    return allCompanies || company(companyId).map(c -> c.allowsBranch(branchId)).orElse(false);
  }

  /**
   * The grant of one company.
   *
   * @param companyId company id
   * @return grant, empty when the company is not listed (or all companies are granted)
   */
  public Optional<CompanyScope> company(Long companyId) {
    return companies.stream().filter(c -> c.companyId().equals(companyId)).findFirst();
  }

  /**
   * The listed company ids.
   *
   * @return ids (empty when all companies are granted)
   */
  public Set<Long> companyIds() {
    return companies.stream().map(CompanyScope::companyId).collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Whether this scope grants nothing more than another one (an administrator grants only inside
   * his own scope).
   *
   * @param outer the wider scope
   * @return true when every company and branch of this scope is in {@code outer}
   */
  public boolean within(UserDataScope outer) {
    return outer.allCompanies()
        || !allCompanies && companies.stream().allMatch(c -> c.within(outer));
  }

  /**
   * The text form, stored on access requests and written to the access change log.
   *
   * @return {@value #ALL_TEXT}, empty, or {@code company:*} / {@code company:branch,branch} joined
   *     by semicolons
   */
  public String text() {
    if (allCompanies) {
      return ALL_TEXT;
    }
    return companies.stream()
        .map(CompanyScope::text)
        .collect(Collectors.joining(COMPANY_SEPARATOR));
  }

  /**
   * Reads the text form.
   *
   * @param text text form; null or blank for a scope that grants nothing
   * @return scope
   * @throws IllegalArgumentException when the text is malformed
   */
  public static UserDataScope parse(String text) {
    if (text == null || text.isBlank()) {
      return NONE;
    }
    String trimmed = text.trim();
    if (ALL_TEXT.equals(trimmed)) {
      return ALL;
    }
    List<CompanyScope> companies = new ArrayList<>();
    for (String part : trimmed.split(COMPANY_SEPARATOR, -1)) {
      companies.add(CompanyScope.parse(part.trim()));
    }
    return of(companies);
  }

  /**
   * One granted company.
   *
   * @param companyId company id
   * @param allBranches true for every branch of the company, present and future
   * @param branchIds granted branches (empty when {@code allBranches}), ordered
   */
  public record CompanyScope(Long companyId, boolean allBranches, List<Long> branchIds) {

    /** Normalises: sorted distinct branches; none when all are granted. */
    public CompanyScope {
      if (companyId == null) {
        throw new IllegalArgumentException("A data scope company needs its id");
      }
      branchIds =
          allBranches || branchIds == null ? List.of() : List.copyOf(new TreeSet<>(branchIds));
    }

    /**
     * A company with all its branches.
     *
     * @param companyId company id
     * @return grant
     */
    public static CompanyScope allBranchesOf(Long companyId) {
      return new CompanyScope(companyId, true, List.of());
    }

    /**
     * A company with some of its branches.
     *
     * @param companyId company id
     * @param branchIds branches
     * @return grant
     */
    public static CompanyScope branchesOf(Long companyId, List<Long> branchIds) {
      return new CompanyScope(companyId, false, branchIds);
    }

    /**
     * Whether the branch is granted.
     *
     * @param branchId branch id
     * @return true when allowed
     */
    public boolean allowsBranch(Long branchId) {
      return allBranches || branchIds.contains(branchId);
    }

    private boolean within(UserDataScope outer) {
      return outer
          .company(companyId)
          .map(o -> o.allBranches() || !allBranches && o.branchIds().containsAll(branchIds))
          .orElse(false);
    }

    private CompanyScope merge(CompanyScope other) {
      if (allBranches || other.allBranches()) {
        return allBranchesOf(companyId);
      }
      Set<Long> both = new LinkedHashSet<>(branchIds);
      both.addAll(other.branchIds());
      return branchesOf(companyId, List.copyOf(both));
    }

    private String text() {
      String branches =
          allBranches
              ? ALL_BRANCHES
              : branchIds.stream()
                  .sorted(Comparator.naturalOrder())
                  .map(String::valueOf)
                  .collect(Collectors.joining(BRANCH_SEPARATOR));
      return companyId + ":" + branches;
    }

    private static CompanyScope parse(String part) {
      int colon = part.indexOf(':');
      if (colon <= 0) {
        throw new IllegalArgumentException("Malformed data scope entry: " + part);
      }
      Long company = Long.valueOf(part.substring(0, colon).trim());
      String branches = part.substring(colon + 1).trim();
      if (ALL_BRANCHES.equals(branches)) {
        return allBranchesOf(company);
      }
      List<Long> ids = new ArrayList<>();
      if (!branches.isEmpty()) {
        for (String b : branches.split(BRANCH_SEPARATOR, -1)) {
          ids.add(Long.valueOf(b.trim()));
        }
      }
      return branchesOf(company, ids);
    }
  }
}
