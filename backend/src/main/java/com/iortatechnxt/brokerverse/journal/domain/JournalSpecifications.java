package com.iortatechnxt.brokerverse.journal.domain;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Builds the dynamic query for {@link JournalSearchCriteria}; null criteria are ignored. */
public final class JournalSpecifications {

  private static final String VALUE_DATE = "valueDate";

  private JournalSpecifications() {}

  /**
   * Creates a specification from search criteria.
   *
   * @param c criteria
   * @return specification
   */
  public static Specification<JournalBatch> matching(JournalSearchCriteria c) {
    return (root, query, cb) -> {
      List<Predicate> p = new ArrayList<>();
      p.add(cb.equal(root.get("companyId"), c.companyId()));
      equal(p, cb, root, "branchId", c.branchId());
      equal(p, cb, root, "status", c.status());
      equal(p, cb, root, "journalType", c.journalType());
      equal(p, cb, root, "sourceModule", blankToNull(c.sourceModule()));
      dateRange(p, cb, root, c.fromDate(), c.toDate());
      if (c.batchNo() != null && !c.batchNo().isBlank()) {
        p.add(cb.like(root.get("batchNo"), c.batchNo().trim() + "%"));
      }
      equalIgnoreCase(p, cb, root, "createdBy", c.inputter());
      equalIgnoreCase(p, cb, root, "authorizedBy", c.authorizer());
      equalIgnoreCase(p, cb, root, "assignedTo", c.assignedTo());
      if (c.minAmount() != null) {
        p.add(cb.greaterThanOrEqualTo(root.get("totalDebit"), c.minAmount()));
      }
      return cb.and(p.toArray(Predicate[]::new));
    };
  }

  private static void equal(
      List<Predicate> p, CriteriaBuilder cb, Root<JournalBatch> root, String field, Object value) {
    if (value != null) {
      p.add(cb.equal(root.get(field), value));
    }
  }

  private static void equalIgnoreCase(
      List<Predicate> p, CriteriaBuilder cb, Root<JournalBatch> root, String field, String value) {
    String v = blankToNull(value);
    if (v != null) {
      p.add(cb.equal(cb.lower(root.get(field)), v.toLowerCase(Locale.ROOT)));
    }
  }

  private static void dateRange(
      List<Predicate> p,
      CriteriaBuilder cb,
      Root<JournalBatch> root,
      LocalDate from,
      LocalDate to) {
    if (from != null) {
      p.add(cb.greaterThanOrEqualTo(root.get(VALUE_DATE), from));
    }
    if (to != null) {
      p.add(cb.lessThanOrEqualTo(root.get(VALUE_DATE), to));
    }
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
