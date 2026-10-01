package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.security.DataScope;
import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import com.iortatechnxt.brokerverse.common.security.UserDataScope.CompanyScope;
import com.iortatechnxt.brokerverse.security.domain.AccessChange;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeActivity;
import com.iortatechnxt.brokerverse.security.domain.AccessSubjectType;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.UserDataScopeGrant;
import com.iortatechnxt.brokerverse.security.domain.UserDataScopeGrantRepository;
import com.iortatechnxt.brokerverse.security.service.OrganizationUnits.BranchUnit;
import com.iortatechnxt.brokerverse.security.service.OrganizationUnits.CompanyUnit;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administration of the data scope of users (docs/architecture/DATA_SCOPE_DESIGN.md): reads a
 * user's scope, validates and replaces it. Every change writes the access change log (activity
 * {@code DATA_SCOPE_CHANGED}, with the request number and approver of an approved access request)
 * and the audit trail in the same transaction, and clears the scope cache.
 */
@Service
@Transactional
public class DataScopeService {

  private static final String USER = "AppUser";
  private static final String ATTRIBUTE = "dataScope";

  private final AppUserRepository users;
  private final UserDataScopeGrantRepository grants;
  private final DataScopeLookup lookup;
  private final DataScope guard;
  private final OrganizationUnits units;
  private final AccessChangeRecorder changes;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param users users
   * @param grants data scope grants
   * @param lookup scope resolution
   * @param guard data scope of the current user
   * @param units companies and branches
   * @param changes access change log
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // the collaborators of one audited change
  public DataScopeService(
      AppUserRepository users,
      UserDataScopeGrantRepository grants,
      DataScopeLookup lookup,
      DataScope guard,
      OrganizationUnits units,
      AccessChangeRecorder changes,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.users = users;
    this.grants = grants;
    this.lookup = lookup;
    this.guard = guard;
    this.units = units;
    this.changes = changes;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The data scope of a user.
   *
   * @param userId user id
   * @return scope
   */
  @Transactional(readOnly = true)
  public UserDataScope scopeOf(Long userId) {
    return lookup.scopeOfUser(user(userId));
  }

  /**
   * The companies and branches the current user may grant: those of his own scope.
   *
   * @return companies with their branches, ordered by code
   */
  @Transactional(readOnly = true)
  public List<CompanyUnit> grantableUnits() {
    UserDataScope own = guard.allowed();
    return units.companies().stream()
        .filter(c -> own.allowsCompany(c.id()))
        .map(
            c ->
                new CompanyUnit(
                    c.id(),
                    c.code(),
                    c.name(),
                    c.branches().stream().filter(b -> own.allowsBranch(c.id(), b.id())).toList()))
        .toList();
  }

  /**
   * Checks a requested scope before it is submitted or applied: known companies, branches of their
   * company, at least one company, at least one branch of a company granted with a branch list, and
   * (when {@code withinOwnScope}) nothing beyond the scope of the current user.
   *
   * @param scope requested scope
   * @param withinOwnScope whether the current user may grant only inside his own scope
   */
  @Transactional(readOnly = true)
  public void validate(UserDataScope scope, boolean withinOwnScope) {
    if (!scope.allCompanies()) {
      requireKnownUnits(scope);
    }
    if (withinOwnScope && !scope.within(guard.allowed())) {
      throw new BusinessRuleException(
          "DATA_SCOPE_BEYOND_OWN",
          "You can grant only companies and branches that are within your own data access");
    }
  }

  private void requireKnownUnits(UserDataScope scope) {
    if (scope.companies().isEmpty()) {
      throw new BusinessRuleException(
          "DATA_SCOPE_EMPTY", "Select at least one company, or give access to all companies");
    }
    Map<Long, CompanyUnit> known = knownUnits();
    scope.companies().forEach(c -> requireKnownCompany(c, known.get(c.companyId())));
  }

  private static void requireKnownCompany(CompanyScope c, CompanyUnit company) {
    if (company == null) {
      throw new BusinessRuleException(
          "DATA_SCOPE_UNKNOWN_COMPANY", "A selected company does not exist");
    }
    if (!c.allBranches() && c.branchIds().isEmpty()) {
      throw new BusinessRuleException(
          "DATA_SCOPE_NO_BRANCH",
          "Select at least one branch of "
              + company.name()
              + ", or give access to all its branches");
    }
    List<Long> branchIds = company.branches().stream().map(BranchUnit::id).toList();
    if (!branchIds.containsAll(c.branchIds())) {
      throw new BusinessRuleException(
          "DATA_SCOPE_UNKNOWN_BRANCH", "A selected branch is not a branch of " + company.name());
    }
  }

  /**
   * Replaces the data scope of a user. A direct change (no request) may grant only inside the scope
   * of the administrator; nobody changes his own scope.
   *
   * @param user user
   * @param scope new scope
   * @param authority approved request and approver, or {@link ChangeAuthority#DIRECT}
   * @return the scope now in force
   */
  @CacheEvict(cacheNames = SecurityCaches.DATA_SCOPE, allEntries = true)
  public UserDataScope replace(AppUser user, UserDataScope scope, ChangeAuthority authority) {
    if (CurrentUser.sameUser(user.getUsername(), currentUser.username())) {
      throw new BusinessRuleException(
          "DATA_SCOPE_SELF_CHANGE", "You cannot change your own data access");
    }
    validate(scope, authority.isDirect());
    UserDataScope before = lookup.scopeOfUser(user);
    if (before.equals(scope)) {
      return before;
    }
    grants.deleteAll(grants.findByUserIdOrderByCompanyIdAscBranchIdAsc(user.getId()));
    grants.flush();
    user.setAllCompanies(scope.allCompanies());
    Instant now = clock.instant();
    String by = currentUser.username();
    for (CompanyScope c : scope.companies()) {
      if (c.allBranches()) {
        grants.save(new UserDataScopeGrant(user.getId(), c.companyId(), null, now, by));
      } else {
        c.branchIds()
            .forEach(
                b -> grants.save(new UserDataScopeGrant(user.getId(), c.companyId(), b, now, by)));
      }
    }
    Map<Long, CompanyUnit> known = knownUnits();
    changes.record(
        new AccessChange(
            AccessSubjectType.USER,
            user.getUsername(),
            AccessChangeActivity.DATA_SCOPE_CHANGED,
            ATTRIBUTE,
            describe(before, known),
            describe(scope, known)),
        authority);
    audit.record(
        USER,
        user.getUsername(),
        AuditAction.UPDATE,
        "Data access set to " + describe(scope, known) + requestText(authority));
    return scope;
  }

  /**
   * Replaces the data scope of a user by id (direct change on the Users screen).
   *
   * @param userId user id
   * @param scope new scope
   * @return the scope now in force
   */
  public UserDataScope replace(Long userId, UserDataScope scope) {
    return replace(user(userId), scope, ChangeAuthority.DIRECT);
  }

  /**
   * The scope in business words: "All companies", or company codes with "all branches" or their
   * branch codes.
   *
   * @param scope scope
   * @return text
   */
  @Transactional(readOnly = true)
  public String describe(UserDataScope scope) {
    return describe(scope, knownUnits());
  }

  private static String describe(UserDataScope scope, Map<Long, CompanyUnit> known) {
    if (scope.allCompanies()) {
      return "All companies";
    }
    if (scope.companies().isEmpty()) {
      return "No company";
    }
    return scope.companies().stream()
        .map(c -> companyText(c, known.get(c.companyId())))
        .collect(Collectors.joining("; "));
  }

  private static String companyText(CompanyScope c, CompanyUnit unit) {
    String company = unit == null ? String.valueOf(c.companyId()) : unit.code();
    if (c.allBranches()) {
      return company + " (all branches)";
    }
    Map<Long, String> codes =
        unit == null
            ? Map.of()
            : unit.branches().stream().collect(Collectors.toMap(BranchUnit::id, BranchUnit::code));
    return company
        + ": "
        + c.branchIds().stream()
            .map(b -> codes.getOrDefault(b, String.valueOf(b)))
            .collect(Collectors.joining(", "));
  }

  private Map<Long, CompanyUnit> knownUnits() {
    return units.companies().stream()
        .collect(Collectors.toMap(CompanyUnit::id, Function.identity(), (a, b) -> a));
  }

  private AppUser user(Long id) {
    return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
  }

  private static String requestText(ChangeAuthority authority) {
    return authority.isDirect() ? "" : " (request " + authority.requestNo() + ")";
  }
}
