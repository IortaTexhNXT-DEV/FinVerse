package com.iortatechnxt.finverse.underwriting.domain;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Dynamic queries for policies and endorsements; null criteria are ignored. */
public final class UnderwritingSpecifications {

  private static final String COMPANY_ID = "companyId";
  private static final String WORKFLOW = "workflow";
  private static final String STATUS = "status";
  private static final String POLICY = "policy";
  private static final String ISSUE_DATE = "issueDate";

  private UnderwritingSpecifications() {}

  /**
   * Policy search for the policy list screen.
   *
   * @param c criteria
   * @return specification
   */
  public static Specification<Policy> policies(PolicySearchCriteria c) {
    return (root, query, cb) -> {
      List<Predicate> p = new ArrayList<>();
      p.add(cb.equal(root.get(COMPANY_ID), c.companyId()));
      equal(p, cb, root.get("branchId"), c.branchId());
      equal(p, cb, root.get(WORKFLOW).get(STATUS), c.status());
      equal(p, cb, root.get("product").get("id"), c.productId());
      equal(p, cb, root.get("openCover").get("id"), c.openCoverId());
      String customer = blankToNull(c.customerCode());
      if (customer != null) {
        p.add(cb.equal(root.get("customer").get("code"), customer));
      }
      String text = blankToNull(c.text());
      if (text != null) {
        String like = "%" + text.toLowerCase(Locale.ROOT) + "%";
        p.add(
            cb.or(
                cb.like(cb.lower(root.get("policyNo")), like),
                cb.like(cb.lower(root.get("insuredName")), like)));
      }
      range(p, cb, root.get(ISSUE_DATE), c.fromDate(), c.toDate());
      return cb.and(p.toArray(Predicate[]::new));
    };
  }

  /**
   * Policies (original issues) selected by a date basis, for registers.
   *
   * @param companyId company
   * @param basis date basis
   * @param from from date (null = open)
   * @param to to date (null = open)
   * @param statuses statuses to include
   * @return specification
   */
  public static Specification<Policy> policiesBy(
      Long companyId,
      DateBasis basis,
      LocalDate from,
      LocalDate to,
      Collection<PolicyStatus> statuses) {
    return (root, query, cb) -> {
      List<Predicate> p = new ArrayList<>();
      p.add(cb.equal(root.get(COMPANY_ID), companyId));
      p.add(root.get(WORKFLOW).get(STATUS).in(statuses));
      Path<LocalDate> date =
          switch (basis) {
            case ISSUE -> root.get(ISSUE_DATE);
            case APPROVAL -> root.get(WORKFLOW).get("approvalDate");
            case PERIOD_FROM -> root.get("periodFrom");
          };
      range(p, cb, date, from, to);
      return cb.and(p.toArray(Predicate[]::new));
    };
  }

  /**
   * Endorsements selected by a date basis, for registers.
   *
   * @param companyId company
   * @param basis date basis
   * @param from from date (null = open)
   * @param to to date (null = open)
   * @param statuses statuses to include
   * @return specification
   */
  public static Specification<Endorsement> endorsementsBy(
      Long companyId,
      DateBasis basis,
      LocalDate from,
      LocalDate to,
      Collection<PolicyStatus> statuses) {
    return (root, query, cb) -> {
      List<Predicate> p = new ArrayList<>();
      p.add(cb.equal(root.get(POLICY).get(COMPANY_ID), companyId));
      p.add(root.get(WORKFLOW).get(STATUS).in(statuses));
      Path<LocalDate> date =
          switch (basis) {
            case ISSUE -> root.get(ISSUE_DATE);
            case APPROVAL -> root.get(WORKFLOW).get("approvalDate");
            case PERIOD_FROM -> root.get("effectiveDate");
          };
      range(p, cb, date, from, to);
      return cb.and(p.toArray(Predicate[]::new));
    };
  }

  /**
   * Policies whose current period ends within a range.
   *
   * @param companyId company
   * @param from period end from (null = open)
   * @param to period end to (null = open)
   * @param statuses statuses to include
   * @return specification
   */
  public static Specification<Policy> expiring(
      Long companyId, LocalDate from, LocalDate to, Collection<PolicyStatus> statuses) {
    return (root, query, cb) -> {
      List<Predicate> p = new ArrayList<>();
      p.add(cb.equal(root.get(COMPANY_ID), companyId));
      p.add(root.get(WORKFLOW).get(STATUS).in(statuses));
      range(p, cb, root.get("periodTo"), from, to);
      return cb.and(p.toArray(Predicate[]::new));
    };
  }

  private static void equal(List<Predicate> p, CriteriaBuilder cb, Path<?> path, Object value) {
    if (value != null) {
      p.add(cb.equal(path, value));
    }
  }

  private static void range(
      List<Predicate> p, CriteriaBuilder cb, Path<LocalDate> path, LocalDate from, LocalDate to) {
    if (from != null) {
      p.add(cb.greaterThanOrEqualTo(path, from));
    }
    if (to != null) {
      p.add(cb.lessThanOrEqualTo(path, to));
    }
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
