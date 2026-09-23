package com.iortatechnxt.finverse.claims.domain;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** JPA specifications for claim searches. */
public final class ClaimSpecifications {

  private static final String POLICY = "policy";
  private static final String LOSS = "loss";
  private static final String LOSS_DATE = "lossDate";

  private ClaimSpecifications() {}

  /**
   * Claims matching the list filters.
   *
   * @param c criteria
   * @return specification
   */
  public static Specification<Claim> claims(ClaimSearchCriteria c) {
    return (root, query, cb) -> {
      List<Predicate> p = new ArrayList<>();
      p.add(cb.equal(root.get("companyId"), c.companyId()));
      if (c.branchId() != null) {
        p.add(cb.equal(root.get("branchId"), c.branchId()));
      }
      if (c.status() != null) {
        p.add(cb.equal(root.get("status"), c.status()));
      }
      if (c.businessLine() != null && !c.businessLine().isBlank()) {
        p.add(cb.equal(root.get(POLICY).get("businessLine"), c.businessLine()));
      }
      if (c.policyId() != null) {
        p.add(cb.equal(root.get(POLICY).get("policyId"), c.policyId()));
      }
      addDates(c, root, cb, p);
      addText(c.q(), root, cb, p);
      return cb.and(p.toArray(new Predicate[0]));
    };
  }

  private static void addDates(
      ClaimSearchCriteria c, Root<Claim> root, CriteriaBuilder cb, List<Predicate> p) {
    if (c.lossFrom() != null) {
      p.add(cb.greaterThanOrEqualTo(root.get(LOSS).get(LOSS_DATE), c.lossFrom()));
    }
    if (c.lossTo() != null) {
      p.add(cb.lessThanOrEqualTo(root.get(LOSS).get(LOSS_DATE), c.lossTo()));
    }
  }

  private static void addText(String q, Root<Claim> root, CriteriaBuilder cb, List<Predicate> p) {
    if (q == null || q.isBlank()) {
      return;
    }
    String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    p.add(
        cb.or(
            cb.like(cb.lower(root.get("claimNo")), like),
            cb.like(cb.lower(root.get(POLICY).get("policyNo")), like),
            cb.like(cb.lower(root.get(POLICY).get("insuredName")), like)));
  }
}
