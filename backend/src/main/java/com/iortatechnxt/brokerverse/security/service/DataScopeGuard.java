package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.common.exception.DataScopeDeniedException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.security.DataScope;
import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

/**
 * The central data scope guard (docs/architecture/DATA_SCOPE_DESIGN.md): checks that the signed-in
 * user may act for the company or branch a request names. Fail closed: a user request without a
 * known signed-in user is refused.
 *
 * <p>Exempt, explicitly ({@link #systemProcessing()}): processing that is not an HTTP request
 * dispatched to a controller (scheduled jobs, event consumers, the outbox relay, seed and migration
 * loaders) and the system-to-system APIs under {@value #INTEGRATION_PATH}; both run as the system
 * for every company.
 */
@Service
public class DataScopeGuard implements DataScope {

  /** Path prefix of the system-to-system APIs (gateway-authenticated, own security chain). */
  public static final String INTEGRATION_PATH = "/integration/";

  private final DataScopeLookup lookup;
  private final CurrentUser currentUser;
  private final OrganizationUnits units;

  /**
   * Creates the guard.
   *
   * @param lookup cached scope of a user
   * @param currentUser signed-in user
   * @param units companies and branches (company of a branch)
   */
  public DataScopeGuard(DataScopeLookup lookup, CurrentUser currentUser, OrganizationUnits units) {
    this.lookup = lookup;
    this.currentUser = currentUser;
    this.units = units;
  }

  @Override
  public void requireCompany(Long companyId) {
    if (companyId != null && !allowed().allowsCompany(companyId)) {
      throw DataScopeDeniedException.company();
    }
  }

  @Override
  public void requireBranch(Long companyId, Long branchId) {
    if (branchId == null) {
      requireCompany(companyId);
    } else {
      requireListedBranch(companyId, branchId);
    }
  }

  private void requireListedBranch(Long companyId, Long branchId) {
    UserDataScope scope = allowed();
    if (scope.allCompanies()) {
      return;
    }
    Long company = companyId != null ? companyId : units.companyOfBranch(branchId).orElse(null);
    if (company == null || !scope.allowsCompany(company)) {
      throw companyId == null
          ? DataScopeDeniedException.branch()
          : DataScopeDeniedException.company();
    }
    if (!scope.allowsBranch(company, branchId)) {
      throw DataScopeDeniedException.branch();
    }
  }

  @Override
  public UserDataScope allowed() {
    if (systemProcessing()) {
      return UserDataScope.ALL;
    }
    return currentUser.optionalUsername().map(lookup::scopeOf).orElse(UserDataScope.NONE);
  }

  /**
   * Whether the current work runs as the system: no HTTP request is being dispatched to a
   * controller on this thread, or the request is a system-to-system API call.
   *
   * @return true for system processing
   */
  public static boolean systemProcessing() {
    return currentRequest()
        .filter(r -> r.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE) != null)
        .map(r -> r.getRequestURI().substring(r.getContextPath().length()))
        .map(path -> path.startsWith(INTEGRATION_PATH))
        .orElse(true);
  }

  private static Optional<HttpServletRequest> currentRequest() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    return attributes instanceof ServletRequestAttributes servlet
        ? Optional.of(servlet.getRequest())
        : Optional.empty();
  }
}
