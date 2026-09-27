package com.iortatechnxt.brokerverse.collections.worklist.service;

import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter.Ranges;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter.Scope;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter.Work;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** The worklist filters as a JPA specification (BRCLXN.001-012; server-side filtering). */
final class WorklistSpecifications {

  static final String CLASSIFICATION = "classification";
  static final String PARTIES = "parties";
  static final String FIGURES = "figures";
  static final String NET_OUTSTANDING = "netOutstanding";
  private static final String HANDLER = "currentHandler";

  private WorklistSpecifications() {}

  /**
   * Items of a company matching the filter.
   *
   * @param companyId company
   * @param f filter
   * @return specification
   */
  static Specification<CollectionItem> of(Long companyId, WorklistFilter f) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      where.add(cb.equal(root.get("companyId"), companyId));
      if (!f.statuses().isEmpty()) {
        where.add(root.get("status").in(f.statuses()));
      }
      scope(where, cb, root, f.scope());
      work(where, cb, root, f.work());
      ranges(where, cb, root, f.ranges());
      text(where, cb, root, f.text());
      return cb.and(where.toArray(Predicate[]::new));
    };
  }

  private static void scope(
      List<Predicate> where, CriteriaBuilder cb, Root<CollectionItem> root, Scope s) {
    Path<Object> c = root.get(CLASSIFICATION);
    eq(where, cb, c.get("segment"), s.segment());
    eq(where, cb, c.get("salesUnit"), s.salesUnit());
    eq(where, cb, c.get("unitHeadUsername"), s.unitHead());
    eq(where, cb, c.get("aoUsername"), s.aoUsername());
    eq(where, cb, root.get(PARTIES).get("clientCode"), s.clientCode());
  }

  private static void work(
      List<Predicate> where, CriteriaBuilder cb, Root<CollectionItem> root, Work w) {
    eq(where, cb, root.get(HANDLER), w.handler());
    eq(where, cb, root.get(FIGURES).get("agingBracket"), w.agingBracket());
    eq(where, cb, root.get("category"), w.category());
    eq(where, cb, root.get("dispositionCode"), w.dispositionCode());
    eq(where, cb, root.get("promiseStatus"), w.promiseStatus());
    if (w.escalated() != null) {
      Path<Object> level = root.get("escalationLevel");
      where.add(w.escalated() ? cb.isNotNull(level) : cb.isNull(level));
    }
    if (Boolean.TRUE.equals(w.unassigned())) {
      where.add(cb.isNull(root.get(HANDLER)));
    }
  }

  private static void ranges(
      List<Predicate> where, CriteriaBuilder cb, Root<CollectionItem> root, Ranges r) {
    Path<Object> figures = root.get(FIGURES);
    between(where, cb, figures.get(NET_OUTSTANDING), r.amountFrom(), r.amountTo());
    between(where, cb, figures.get("agingDays"), r.agingFrom(), r.agingTo());
  }

  private static void eq(List<Predicate> where, CriteriaBuilder cb, Path<String> path, String v) {
    if (v != null && !v.isBlank()) {
      where.add(cb.equal(cb.upper(path), v.strip().toUpperCase(Locale.ROOT)));
    }
  }

  private static <T extends Comparable<? super T>> void between(
      List<Predicate> where, CriteriaBuilder cb, Expression<T> value, T from, T to) {
    if (from != null) {
      where.add(cb.greaterThanOrEqualTo(value, from));
    }
    if (to != null) {
      where.add(cb.lessThanOrEqualTo(value, to));
    }
  }

  private static void text(
      List<Predicate> where, CriteriaBuilder cb, Root<CollectionItem> root, String text) {
    if (text == null || text.isBlank()) {
      return;
    }
    String like = "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
    Path<Object> p = root.get(PARTIES);
    where.add(
        cb.or(
            cb.like(cb.lower(root.get("invoiceNo")), like),
            cb.like(cb.lower(p.get("arn")), like),
            cb.like(cb.lower(p.get("policyNo")), like),
            cb.like(cb.lower(p.get("clientCode")), like),
            cb.like(cb.lower(p.get("assuredName")), like)));
  }
}
