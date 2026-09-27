package com.iortatechnxt.brokerverse.renewal.candidate.service;

import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Codes;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Flags;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Tab;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAssignment;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.report.core.CodeSet;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA filters of the renewal lists (FR-RN-011, 012, 040, 062): tab, expiry range, search, the
 * include / "all except" criteria and the quick filters. The data scope is added by the caller.
 */
final class CandidateSpecifications {

  private static final String SNAPSHOT = "snapshot";
  private static final String PRODUCT = "product";
  private static final String SALES = "sales";
  private static final String FLAGS = "flags";
  private static final String STAGE = "stage";

  private static final Set<RenewalStage> CLOSED =
      EnumSet.of(RenewalStage.RENEWED, RenewalStage.CLOSED);

  private static final Map<Tab, RenewalDisposition> DISPOSITION_TABS =
      Map.of(
          Tab.FOR_RENEWAL, RenewalDisposition.FOR_RENEWAL,
          Tab.FOR_QUOTATION, RenewalDisposition.FOR_QUOTATION,
          Tab.FOR_PROPOSAL, RenewalDisposition.FOR_PROPOSAL,
          Tab.NOT_FOR_RENEWAL, RenewalDisposition.NOT_FOR_RENEWAL,
          Tab.LOST_BUSINESS, RenewalDisposition.LOST_BUSINESS);

  private static final Map<Tab, RenewalStage> STAGE_TABS = stageTabs();

  private CandidateSpecifications() {}

  private static Map<Tab, RenewalStage> stageTabs() {
    Map<Tab, RenewalStage> map = new EnumMap<>(Tab.class);
    map.put(Tab.EXTRACTED, RenewalStage.EXTRACTED);
    map.put(Tab.UNASSIGNED, RenewalStage.UNASSIGNED);
    map.put(Tab.REVIEW, RenewalStage.FOR_TL_REVIEW);
    map.put(Tab.TRANSFER_PENDING, RenewalStage.TRANSFER_PENDING);
    map.put(Tab.FOR_PROCESSING, RenewalStage.FOR_PROCESSING);
    map.put(Tab.IN_PROCESSING, RenewalStage.IN_PROCESSING);
    map.put(Tab.WITH_INSURER, RenewalStage.WITH_INSURER);
    map.put(Tab.INSURER_RESPONDED, RenewalStage.RA_READY);
    map.put(Tab.RA_READY, RenewalStage.RA_READY);
    map.put(Tab.RA_GENERATED, RenewalStage.RA_GENERATED);
    map.put(Tab.RA_SENT, RenewalStage.RA_SENT);
    map.put(Tab.LETTER_PENDING, RenewalStage.LETTER_PENDING);
    map.put(Tab.NB_PATH, RenewalStage.NB_PATH);
    return map;
  }

  /**
   * The filter of a list.
   *
   * @param f criteria
   * @param user current user (quick filter "assigned to me")
   * @param today business date
   * @return specification
   */
  static Specification<RenewalCandidate> of(CandidateFilter f, String user, LocalDate today) {
    return (root, query, cb) -> {
      List<Predicate> p = new ArrayList<>();
      p.add(cb.equal(root.get("companyId"), f.companyId()));
      p.add(tab(root, cb, f.tab() == null ? Tab.ALL : f.tab()));
      range(root, cb, f, p);
      search(root, cb, f.search(), p);
      codes(root, cb, f.codes() == null ? Codes.NONE : f.codes(), p);
      Flags flags = f.flags() == null ? Flags.NONE : f.flags();
      flags(root, cb, flags, today, p);
      if (flags.assignedToMe()) {
        Subquery<Long> assigned = query.subquery(Long.class);
        var a = assigned.from(RenewalAssignment.class);
        assigned
            .select(a.get("candidateId"))
            .where(
                cb.equal(a.get("username"), user),
                cb.equal(a.get("role"), RenewalAssignment.Role.AO));
        p.add(cb.or(cb.equal(root.get("assignedAo"), user), root.get("id").in(assigned)));
      }
      return cb.and(p.toArray(Predicate[]::new));
    };
  }

  private static Predicate tab(Root<RenewalCandidate> root, CriteriaBuilder cb, Tab tab) {
    RenewalDisposition disposition = DISPOSITION_TABS.get(tab);
    if (disposition != null) {
      return cb.equal(root.get("disposition").get("code"), disposition);
    }
    RenewalStage stage = STAGE_TABS.get(tab);
    if (stage != null) {
      return cb.equal(root.get(STAGE), stage);
    }
    return switch (tab) {
      case EXCEPTIONS ->
          cb.and(
              cb.equal(root.get("bucket"), Bucket.EXCEPTION), cb.not(root.get(STAGE).in(CLOSED)));
      case RETURNED -> cb.isTrue(root.get(FLAGS).get("returned"));
      case NRNS -> cb.isTrue(root.get(FLAGS).get("nrns"));
      case CLOSED -> root.get(STAGE).in(CLOSED);
      default -> cb.conjunction();
    };
  }

  private static void range(
      Root<RenewalCandidate> root, CriteriaBuilder cb, CandidateFilter f, List<Predicate> p) {
    Path<LocalDate> expiry = root.get(SNAPSHOT).get("expiryDate");
    if (f.expiryFrom() != null) {
      p.add(cb.greaterThanOrEqualTo(expiry, f.expiryFrom()));
    }
    if (f.expiryTo() != null) {
      p.add(cb.lessThanOrEqualTo(expiry, f.expiryTo()));
    }
  }

  private static void search(
      Root<RenewalCandidate> root, CriteriaBuilder cb, String text, List<Predicate> p) {
    if (text != null && !text.isBlank()) {
      p.add(cb.like(root.get("searchText"), "%" + text.strip().toLowerCase(Locale.ROOT) + "%"));
    }
  }

  private static void codes(
      Root<RenewalCandidate> root, CriteriaBuilder cb, Codes c, List<Predicate> p) {
    Path<Object> product = root.get(SNAPSHOT).get(PRODUCT);
    Path<Object> sales = root.get(SNAPSHOT).get(SALES);
    p.add(in(cb, sales.get("unitHead"), c.unitHead()));
    p.add(in(cb, product.get("businessOrigin"), c.origin()));
    p.add(in(cb, product.get("accountType"), c.accountType()));
    p.add(in(cb, sales.get("regionCode"), c.region()));
    p.add(in(cb, sales.get("departmentCode"), c.department()));
    p.add(in(cb, sales.get("branchCode"), c.branch()));
    p.add(in(cb, product.get("productCode"), c.riskCode()));
    p.add(in(cb, product.get("segment"), c.segment()));
    p.add(in(cb, sales.get("accountOfficer"), c.officer()));
    p.add(in(cb, root.get(SNAPSHOT).get("insurerCode"), c.insurer()));
    p.add(enumIn(cb, root.get("bucket"), c.bucket(), Bucket.class));
    p.add(
        enumIn(cb, root.get("disposition").get("code"), c.disposition(), RenewalDisposition.class));
    p.add(enumIn(cb, root.get(STAGE), c.stage(), RenewalStage.class));
  }

  private static void flags(
      Root<RenewalCandidate> root,
      CriteriaBuilder cb,
      Flags f,
      LocalDate today,
      List<Predicate> p) {
    Path<Object> flags = root.get(FLAGS);
    if (f.returned()) {
      p.add(cb.isTrue(flags.get("returned")));
    }
    if (f.nrns()) {
      p.add(cb.isTrue(flags.get("nrns")));
    }
    if (f.urgent()) {
      p.add(cb.isTrue(flags.get("urgent")));
    }
    if (f.kycDue()) {
      p.add(cb.isTrue(flags.get("kycDue")));
    }
    if (f.assignedPo() != null && !f.assignedPo().isBlank()) {
      p.add(cb.equal(root.get("assignedPo"), f.assignedPo()));
    }
    if (f.dueWithinDays() != null) {
      p.add(
          cb.lessThanOrEqualTo(
              root.get(SNAPSHOT).<LocalDate>get("expiryDate"), today.plusDays(f.dueWithinDays())));
      p.add(cb.not(root.get(STAGE).in(CLOSED)));
    }
    if (f.disposed()) {
      p.add(cb.isNotNull(root.get("disposition").get("code")));
    }
  }

  private static Predicate in(CriteriaBuilder cb, Path<Object> path, CodeSet set) {
    if (set == null || set.isAll()) {
      return cb.conjunction();
    }
    Predicate listed = path.in(set.values());
    return set.exclude() ? cb.or(cb.isNull(path), cb.not(listed)) : listed;
  }

  private static <E extends Enum<E>> Predicate enumIn(
      CriteriaBuilder cb, Path<Object> path, CodeSet set, Class<E> type) {
    if (set == null || set.isAll()) {
      return cb.conjunction();
    }
    List<E> values = new ArrayList<>();
    for (String v : set.values()) {
      for (E e : type.getEnumConstants()) {
        if (e.name().equals(v)) {
          values.add(e);
        }
      }
    }
    Predicate listed = values.isEmpty() ? cb.disjunction() : path.in(values);
    return set.exclude() ? cb.or(cb.isNull(path), cb.not(listed)) : listed;
  }
}
