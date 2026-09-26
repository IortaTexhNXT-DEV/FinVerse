package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestSearch.Scope;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Who sees which access request (FR-UA-018 R1, FR-UA-010 R2): a draft only its creator; every other
 * request its creator, its approvers, the second approvers (PENDING_SECOND), the System
 * Administrators (FOR_IMPLEMENTATION) and the holders of ACCESS_APPROVE, USER_MANAGE or AUDIT_VIEW.
 * Also builds the work-list query of each tab.
 */
@Component
public class AccessRequestVisibility {

  private static final String STATUS = "status";
  private static final String CREATED_BY = "createdBy";
  private static final String ASSIGNED = "assignedApprover";

  private final CurrentUser currentUser;
  private final AccessSettings settings;

  /**
   * Creates the component.
   *
   * @param currentUser current user
   * @param settings parameters (any approver)
   */
  public AccessRequestVisibility(CurrentUser currentUser, AccessSettings settings) {
    this.currentUser = currentUser;
    this.settings = settings;
  }

  /**
   * Whether the viewer sees every request (not the drafts of others).
   *
   * @return true with ACCESS_APPROVE, USER_MANAGE or AUDIT_VIEW
   */
  public boolean seesAll() {
    return currentUser.hasAuthority(AccessApprovers.ACCESS_APPROVE)
        || currentUser.hasAuthority("USER_MANAGE")
        || currentUser.hasAuthority("AUDIT_VIEW");
  }

  /**
   * Whether the viewer may open a request.
   *
   * @param r request
   * @return true when visible
   */
  public boolean canSee(AccessRequest r) {
    String me = currentUser.username();
    boolean creator = CurrentUser.sameUser(me, r.getCreatedBy());
    if (r.getStatus() == AccessRequestStatus.DRAFT || creator) {
      return creator;
    }
    return seesAll()
        || CurrentUser.sameUser(me, r.getAssignedApprover())
        || r.getApprovers().stream().anyMatch(a -> CurrentUser.sameUser(me, a.getApprover()))
        || queueOf(r.getStatus());
  }

  private boolean queueOf(AccessRequestStatus status) {
    return status == AccessRequestStatus.PENDING_SECOND
            && currentUser.hasAuthority("UAM_SECOND_APPROVE")
        || status == AccessRequestStatus.FOR_IMPLEMENTATION
            && currentUser.hasAuthority("ROLE_MANAGE");
  }

  /**
   * The query of a work-list tab with its filters.
   *
   * @param search filter
   * @return specification
   */
  public Specification<AccessRequest> specification(AccessRequestSearch search) {
    String me = currentUser.username().toLowerCase(Locale.ROOT);
    return (root, query, cb) -> {
      List<Predicate> p = new ArrayList<>();
      p.add(
          cb.or(
              cb.notEqual(root.get(STATUS), AccessRequestStatus.DRAFT),
              cb.equal(cb.lower(root.get(CREATED_BY)), me)));
      p.add(scope(search.scope(), root, cb, me));
      p.addAll(filters(search, root, cb));
      return cb.and(p.toArray(Predicate[]::new));
    };
  }

  private Predicate scope(Scope scope, Root<AccessRequest> root, CriteriaBuilder cb, String me) {
    Predicate mine = cb.equal(cb.lower(root.get(CREATED_BY)), me);
    return switch (scope) {
      case MINE -> mine;
      case ASSIGNED -> assigned(root, cb, me);
      case SECOND ->
          currentUser.hasAuthority("UAM_SECOND_APPROVE")
              ? cb.equal(root.get(STATUS), AccessRequestStatus.PENDING_SECOND)
              : cb.disjunction();
      case IMPLEMENTATION ->
          currentUser.hasAuthority("ROLE_MANAGE")
              ? cb.equal(root.get(STATUS), AccessRequestStatus.FOR_IMPLEMENTATION)
              : cb.disjunction();
      case ALL ->
          seesAll() ? cb.conjunction() : cb.or(mine, cb.equal(cb.lower(root.get(ASSIGNED)), me));
    };
  }

  private Predicate assigned(Root<AccessRequest> root, CriteriaBuilder cb, String me) {
    Predicate pending = cb.equal(root.get(STATUS), AccessRequestStatus.PENDING);
    Predicate chosen = cb.equal(cb.lower(root.get(ASSIGNED)), me);
    if (settings.anyApprover() && currentUser.hasAuthority(AccessApprovers.ACCESS_APPROVE)) {
      return cb.and(pending, cb.notEqual(cb.lower(root.get(CREATED_BY)), me));
    }
    return cb.and(pending, chosen);
  }

  private static List<Predicate> filters(
      AccessRequestSearch s, Root<AccessRequest> root, CriteriaBuilder cb) {
    List<Predicate> p = new ArrayList<>();
    if (s.status() != null) {
      p.add(cb.equal(root.get(STATUS), s.status()));
    }
    if (s.type() != null) {
      p.add(cb.equal(root.get("requestType"), s.type()));
    }
    if (s.groupProfiles() != null) {
      p.add(groupProfiles(root, s.groupProfiles()));
    }
    if (s.text() != null && !s.text().isBlank()) {
      String like = "%" + s.text().trim().toLowerCase(Locale.ROOT) + "%";
      p.add(
          cb.or(
              cb.like(cb.lower(root.get("username")), like),
              cb.like(cb.lower(root.get("roleCode")), like),
              cb.like(cb.lower(root.get("requestNo")), like)));
    }
    p.addAll(people(s, root, cb));
    p.addAll(dates(s.from(), s.to(), root, cb));
    return p;
  }

  private static Predicate groupProfiles(Root<AccessRequest> root, boolean groupProfiles) {
    List<AccessRequestType> types =
        Arrays.stream(AccessRequestType.values())
            .filter(t -> t.isGroupProfile() == groupProfiles)
            .toList();
    return root.get("requestType").in(types);
  }

  private static List<Predicate> people(
      AccessRequestSearch s, Root<AccessRequest> root, CriteriaBuilder cb) {
    List<Predicate> p = new ArrayList<>();
    if (s.requester() != null && !s.requester().isBlank()) {
      p.add(
          cb.equal(cb.lower(root.get(CREATED_BY)), s.requester().trim().toLowerCase(Locale.ROOT)));
    }
    if (s.approver() != null && !s.approver().isBlank()) {
      String a = s.approver().trim().toLowerCase(Locale.ROOT);
      p.add(
          cb.or(
              cb.equal(cb.lower(root.get(ASSIGNED)), a),
              cb.equal(cb.lower(root.get("decidedBy")), a)));
    }
    return p;
  }

  private static List<Predicate> dates(
      LocalDate from, LocalDate to, Root<AccessRequest> root, CriteriaBuilder cb) {
    List<Predicate> p = new ArrayList<>();
    if (from != null) {
      p.add(
          cb.greaterThanOrEqualTo(
              root.<Instant>get("createdAt"), from.atStartOfDay(WorkingHours.ZONE).toInstant()));
    }
    if (to != null) {
      p.add(
          cb.lessThan(
              root.<Instant>get("createdAt"),
              to.plusDays(1).atStartOfDay(WorkingHours.ZONE).toInstant()));
    }
    return p;
  }
}
