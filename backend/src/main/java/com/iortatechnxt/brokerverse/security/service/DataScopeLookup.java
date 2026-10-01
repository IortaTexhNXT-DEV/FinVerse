package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import com.iortatechnxt.brokerverse.common.security.UserDataScope.CompanyScope;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.UserDataScopeGrant;
import com.iortatechnxt.brokerverse.security.domain.UserDataScopeGrantRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cached resolution of a user's data scope ({@link SecurityCaches#DATA_SCOPE}), read by the guard
 * on every request that names a company. Fail closed: an unknown user has the scope {@link
 * UserDataScope#NONE}.
 */
@Service
public class DataScopeLookup {

  private final AppUserRepository users;
  private final UserDataScopeGrantRepository grants;

  /**
   * Creates the lookup.
   *
   * @param users users
   * @param grants data scope grants
   */
  public DataScopeLookup(AppUserRepository users, UserDataScopeGrantRepository grants) {
    this.users = users;
    this.grants = grants;
  }

  /**
   * The data scope of a user.
   *
   * @param username user name (case-insensitive)
   * @return scope; {@link UserDataScope#NONE} for an unknown user
   */
  @Cacheable(cacheNames = SecurityCaches.DATA_SCOPE, key = "#root.target.key(#username)")
  @Transactional(readOnly = true)
  public UserDataScope scopeOf(String username) {
    return users
        .findByUsernameIgnoreCase(username)
        .map(this::scopeOfUser)
        .orElse(UserDataScope.NONE);
  }

  /**
   * The data scope of a loaded user (not cached).
   *
   * @param user user
   * @return scope
   */
  @Transactional(readOnly = true)
  public UserDataScope scopeOfUser(AppUser user) {
    if (user.isAllCompanies()) {
      return UserDataScope.ALL;
    }
    return toScope(grants.findByUserIdOrderByCompanyIdAscBranchIdAsc(user.getId()));
  }

  /**
   * Cache key of a user name.
   *
   * @param username user name
   * @return lower-case key
   */
  public String key(String username) {
    return username == null ? "" : username.toLowerCase(Locale.ROOT);
  }

  /**
   * The scope written by a list of grants.
   *
   * @param rows grants of one user
   * @return scope of listed companies
   */
  static UserDataScope toScope(List<UserDataScopeGrant> rows) {
    Map<Long, List<Long>> branches = new TreeMap<>();
    Map<Long, Boolean> allBranches = new TreeMap<>();
    for (UserDataScopeGrant g : rows) {
      branches.computeIfAbsent(g.getCompanyId(), k -> new ArrayList<>());
      if (g.getBranchId() == null) {
        allBranches.put(g.getCompanyId(), Boolean.TRUE);
      } else {
        branches.get(g.getCompanyId()).add(g.getBranchId());
      }
    }
    List<CompanyScope> companies = new ArrayList<>();
    branches.forEach(
        (company, ids) ->
            companies.add(
                allBranches.containsKey(company)
                    ? CompanyScope.allBranchesOf(company)
                    : CompanyScope.branchesOf(company, ids)));
    return UserDataScope.of(companies);
  }
}
