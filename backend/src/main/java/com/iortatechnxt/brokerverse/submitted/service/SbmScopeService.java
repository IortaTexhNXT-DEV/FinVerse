package com.iortatechnxt.brokerverse.submitted.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmUserScope;
import com.iortatechnxt.brokerverse.submitted.domain.SbmUserScopeRepository;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The data scope of the user (BRIDSP-28; SUBMITTED_POLICIES_DESIGN section 6.2): the segments of
 * the user's scope row (all when none) and, for the roles of {@code SBM_OWN_RECORDS_ROLES} (the
 * account officers) or a scope row that says so, only the records the user handles or whose renewal
 * account the user owns. Every list, record, report and extract applies it; background jobs (no
 * signed-in user) see everything.
 */
@Service
@Transactional(readOnly = true)
public class SbmScopeService {

  /** Parameter: roles that see their own records only. */
  public static final String OWN_ROLES = "SBM_OWN_RECORDS_ROLES";

  private final SbmUserScopeRepository scopes;
  private final UserDirectory users;
  private final SystemParameterService parameters;
  private final CurrentUser currentUser;

  /**
   * Creates the service.
   *
   * @param scopes user scopes
   * @param users user directory (roles)
   * @param parameters business parameters
   * @param currentUser current user
   */
  public SbmScopeService(
      SbmUserScopeRepository scopes,
      UserDirectory users,
      SystemParameterService parameters,
      CurrentUser currentUser) {
    this.scopes = scopes;
    this.users = users;
    this.parameters = parameters;
    this.currentUser = currentUser;
  }

  /**
   * The scope of the signed-in user.
   *
   * @param companyId company
   * @return scope
   */
  public Scope current(Long companyId) {
    return currentUser.optionalUsername().map(u -> of(companyId, u)).orElse(Scope.ALL);
  }

  /**
   * The scope of a user.
   *
   * @param companyId company
   * @param username user
   * @return scope
   */
  public Scope of(Long companyId, String username) {
    return scopes
        .findByCompanyIdAndUsername(companyId, username)
        .map(s -> new Scope(s.segmentList(), s.isOwnRecordsOnly(), username))
        .orElseGet(() -> new Scope(List.of(), ownByRole(username), username));
  }

  private boolean ownByRole(String username) {
    Set<String> roles = users.roleCodes(username);
    List<String> ownRoles = parameters.items(OWN_ROLES);
    boolean own = roles.stream().anyMatch(ownRoles::contains);
    boolean submittedTeam =
        roles.stream().anyMatch(r -> r.startsWith("SBM_") || "MKT_TL".equals(r));
    return own && !submittedTeam;
  }

  /**
   * Refuses a record outside the user's scope as not found (FRS FR-SP-011).
   *
   * @param policy record
   * @return the record
   */
  public SbmPolicy requireVisible(SbmPolicy policy) {
    if (!current(policy.getCompanyId()).allows(policy)) {
      throw new ResourceNotFoundException("Submitted policy", policy.getSbmNo());
    }
    return policy;
  }

  /**
   * The user scopes of a company (Setup).
   *
   * @param companyId company
   * @return scopes
   */
  public List<SbmUserScope> list(Long companyId) {
    return scopes.findByCompanyIdOrderByUsernameAsc(companyId);
  }

  /**
   * A data scope.
   *
   * @param segments segments, empty for all
   * @param ownOnly own records only
   * @param username user of an own-records scope
   */
  public record Scope(List<String> segments, boolean ownOnly, String username) {

    /** Everything (background jobs). */
    public static final Scope ALL = new Scope(List.of(), false, null);

    /** Defensive copy. */
    public Scope {
      segments = List.copyOf(segments);
    }

    /**
     * Whether the scope shows a record.
     *
     * @param p record
     * @return true when visible
     */
    public boolean allows(SbmPolicy p) {
      boolean segmentOk = segments.isEmpty() || segments.contains(p.getSegment());
      boolean ownOk =
          !ownOnly
              || CurrentUser.sameUser(username, p.getHandlerUsername())
              || CurrentUser.sameUser(username, p.getAoUsername());
      return segmentOk && ownOk;
    }

    /**
     * The scope as a query condition.
     *
     * @return specification
     */
    public Specification<SbmPolicy> specification() {
      return (root, query, cb) -> {
        var predicate = cb.conjunction();
        if (!segments.isEmpty()) {
          predicate = cb.and(predicate, root.get("segment").in(segments));
        }
        if (ownOnly) {
          predicate =
              cb.and(
                  predicate,
                  cb.or(
                      cb.equal(root.get("handlerUsername"), username),
                      cb.equal(root.get("aoUsername"), username)));
        }
        return predicate;
      };
    }

    /**
     * The scope as SQL over the alias {@code p} of {@code sbm_policy} (reports), with the values
     * inlined as parameters of the caller.
     *
     * @return condition starting with " and", empty for everything
     */
    public String sql() {
      StringBuilder sb = new StringBuilder();
      if (!segments.isEmpty()) {
        sb.append(" and p.segment in (:scopeSegments)");
      }
      if (ownOnly) {
        sb.append(" and (p.handler_username = :scopeUser or p.ao_username = :scopeUser)");
      }
      return sb.toString();
    }
  }
}
