package com.iortatechnxt.brokerverse.renewal.service;

import com.iortatechnxt.brokerverse.catalog.domain.SalesUnit;
import com.iortatechnxt.brokerverse.catalog.domain.SalesUnitRepository;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAssignment;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The data scope of a Renewal user (FR-RN-002; RENEWAL_DESIGN section 6.2), applied by the server
 * in every list, record, report and export:
 *
 * <ul>
 *   <li>Processing, LAMD, Contact Center, set-up and audit users see every renewal of the company
 *       (Processing is central); LAMD and Contact Center see a projection without premium columns
 *       (RQ21).
 *   <li>A Marketing Team Leader ({@code RNW_REVIEW}) sees the renewals of his sales units: his own
 *       team and every unit under a unit he heads.
 *   <li>A Marketing AO sees the renewals ever assigned to him (BRRN.011).
 * </ul>
 *
 * <p>Jobs (no user) see everything.
 */
@Component
@Transactional(readOnly = true)
public class RenewalScope {

  private final CurrentUser currentUser;
  private final SalesOrganisationService sales;
  private final SalesUnitRepository units;

  /**
   * Creates the scope.
   *
   * @param currentUser current user
   * @param sales sales organisation (the user's team)
   * @param units sales units (units headed by the user)
   */
  public RenewalScope(
      CurrentUser currentUser, SalesOrganisationService sales, SalesUnitRepository units) {
    this.currentUser = currentUser;
    this.sales = sales;
    this.units = units;
  }

  /**
   * The scope of the current user.
   *
   * @param companyId company
   * @return scope
   */
  public Scope current(Long companyId) {
    if (currentUser.optionalUsername().isEmpty()) {
      return Scope.all(false);
    }
    String user = currentUser.username();
    boolean hidePremium =
        (has(RenewalCodes.LAMD_UPLOAD) || has(RenewalCodes.FOLLOWUP))
            && !has(RenewalCodes.DISPOSE)
            && !has(RenewalCodes.PROCESS);
    if (!has(RenewalCodes.PROCESS) && has(RenewalCodes.REVIEW)) {
      return new Scope(Kind.UNITS, unitsOf(companyId, user), user, hidePremium);
    }
    if (!has(RenewalCodes.PROCESS) && has(RenewalCodes.DISPOSE)) {
      return new Scope(Kind.ASSIGNED, Set.of(), user, hidePremium);
    }
    return Scope.all(hidePremium);
  }

  /**
   * The sales units a user leads: his own team and every unit under a unit he heads.
   *
   * @param companyId company
   * @param user user name
   * @return unit codes
   */
  public Set<String> unitsOf(Long companyId, String user) {
    Set<String> result = new HashSet<>();
    sales.assignmentOf(companyId, user).map(a -> a.team()).ifPresent(result::add);
    List<SalesUnit> all = units.findByCompanyIdOrderByLevelAscCodeAsc(companyId);
    Deque<String> heads = new ArrayDeque<>();
    all.stream()
        .filter(u -> CurrentUser.sameUser(u.getHeadUsername(), user))
        .map(SalesUnit::getCode)
        .forEach(heads::add);
    Set<String> visited = new HashSet<>();
    while (!heads.isEmpty()) {
      String code = heads.pop();
      if (visited.add(code)) {
        result.add(code);
        all.stream()
            .filter(u -> code.equals(u.getParentCode()))
            .map(SalesUnit::getCode)
            .forEach(heads::add);
      }
    }
    return result;
  }

  /**
   * Refuses a renewal outside the scope as if it did not exist (FR-RN-002 "cannot be opened").
   *
   * @param candidate candidate
   * @param assignedToUser whether the renewal was ever assigned to the user (AO scope)
   */
  public void require(RenewalCandidate candidate, boolean assignedToUser) {
    Scope scope = current(candidate.getCompanyId());
    boolean visible =
        switch (scope.kind()) {
          case ALL -> true;
          case UNITS -> scope.units().contains(candidate.getOwnerUnit());
          case ASSIGNED ->
              assignedToUser || Objects.equals(candidate.getAssignedAo(), scope.username());
        };
    if (!visible) {
      throw new ResourceNotFoundException("Renewal", candidate.getRenewalRef());
    }
  }

  private boolean has(String permission) {
    return currentUser.hasAuthority(permission);
  }

  /** Kind of scope. */
  public enum Kind {
    /** Every renewal of the company. */
    ALL,
    /** The renewals of the user's sales units. */
    UNITS,
    /** The renewals ever assigned to the user. */
    ASSIGNED
  }

  /**
   * The scope of a user.
   *
   * @param kind kind
   * @param units sales units (UNITS)
   * @param username user (ASSIGNED)
   * @param hidePremium whether premium columns are hidden (LAMD, Contact Center)
   */
  public record Scope(Kind kind, Set<String> units, String username, boolean hidePremium) {

    /** Defensive copy. */
    public Scope {
      units = units == null ? Set.of() : Set.copyOf(units);
    }

    /**
     * Every renewal.
     *
     * @param hidePremium whether premium columns are hidden
     * @return scope
     */
    public static Scope all(boolean hidePremium) {
      return new Scope(Kind.ALL, Set.of(), null, hidePremium);
    }

    /**
     * The filter of the scope for a candidate query.
     *
     * @return specification
     */
    public Specification<RenewalCandidate> specification() {
      return switch (kind) {
        case ALL -> (root, query, cb) -> cb.conjunction();
        case UNITS ->
            (root, query, cb) ->
                units.isEmpty() ? cb.disjunction() : root.get("ownerUnit").in(units);
        case ASSIGNED ->
            (root, query, cb) -> {
              Subquery<Long> assigned = query.subquery(Long.class);
              var a = assigned.from(RenewalAssignment.class);
              assigned
                  .select(a.get("candidateId"))
                  .where(
                      cb.equal(a.get("username"), username),
                      cb.equal(a.get("role"), RenewalAssignment.Role.AO));
              return cb.or(cb.equal(root.get("assignedAo"), username), root.get("id").in(assigned));
            };
      };
    }
  }
}
