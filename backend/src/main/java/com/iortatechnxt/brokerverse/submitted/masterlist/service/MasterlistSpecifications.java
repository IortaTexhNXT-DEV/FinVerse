package com.iortatechnxt.brokerverse.submitted.masterlist.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.submitted.domain.SbmBusinessType;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistFilter.Tab;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

/** JPA filters of the masterlist (FRS FR-SP-010, 012); the data scope is added by the caller. */
final class MasterlistSpecifications {

  private static final String STATUS = "status";
  private static final String TERMS = "terms";

  private static final Set<SbmPolicyStatus> RENEWAL =
      EnumSet.of(
          SbmPolicyStatus.FOR_RENEWAL,
          SbmPolicyStatus.RENEWAL_IN_PROGRESS,
          SbmPolicyStatus.PLACED,
          SbmPolicyStatus.BOOKED);

  private MasterlistSpecifications() {}

  /**
   * The filter of a list.
   *
   * @param f criteria
   * @return specification
   */
  static Specification<SbmPolicy> of(MasterlistFilter f) {
    return (root, query, cb) -> {
      List<Predicate> all = new ArrayList<>();
      all.add(cb.equal(root.get("companyId"), f.companyId()));
      tab(f.tab(), root, cb, all);
      text(f.text(), root, cb, all);
      equal(root.get("segment"), f.segment(), cb, all);
      if (f.businessType() != null && !f.businessType().isBlank()) {
        all.add(cb.equal(root.get("businessType"), SbmBusinessType.valueOf(f.businessType())));
      }
      equal(root.get("bucket"), f.bucket(), cb, all);
      equal(root.get(TERMS).get("insurerCode"), f.insurerCode(), cb, all);
      equal(root.get("handlerUsername"), f.handler(), cb, all);
      equal(root.get("conversionStatus"), f.conversionStatus(), cb, all);
      if (f.expiryMonth() != null) {
        all.add(cb.equal(root.get("renewalMonth"), f.expiryMonth().toString()));
      }
      if (f.migrated() != null) {
        all.add(cb.equal(root.get("migrated"), f.migrated()));
      }
      if (f.changedSince() != null) {
        all.add(
            cb.greaterThanOrEqualTo(
                cb.coalesce(root.get("updatedAt"), root.get("createdAt")),
                BusinessClock.startOf(f.changedSince())));
      }
      return cb.and(all.toArray(Predicate[]::new));
    };
  }

  private static void tab(Tab tab, Root<SbmPolicy> root, CriteriaBuilder cb, List<Predicate> all) {
    if (tab == null) {
      return;
    }
    switch (tab) {
      case FOR_VALIDATION -> all.add(cb.equal(root.get(STATUS), SbmPolicyStatus.RECEIVED));
      case CLASSIFIED ->
          all.add(root.get(STATUS).in(SbmPolicyStatus.CLASSIFIED, SbmPolicyStatus.IN_REVIEW));
      case FOR_RENEWAL -> all.add(root.get(STATUS).in(RENEWAL));
      case MANUAL_DISPOSITION ->
          all.add(cb.equal(root.get(STATUS), SbmPolicyStatus.FOR_MANUAL_DISPOSITION));
      case NON_RENEWAL -> all.add(cb.equal(root.get(STATUS), SbmPolicyStatus.EXCLUDED));
      case FALLOUT -> all.add(cb.isTrue(root.get("fallout")));
      default -> {
        // ALL: no condition
      }
    }
  }

  private static void text(
      String text, Root<SbmPolicy> root, CriteriaBuilder cb, List<Predicate> all) {
    if (text == null || text.isBlank()) {
      return;
    }
    String like = "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
    all.add(
        cb.or(
            cb.like(cb.lower(root.get("sbmNo")), like),
            cb.like(cb.lower(root.get("loan").get("pnNo")), like),
            cb.like(cb.lower(root.get(TERMS).get("policyNo")), like),
            cb.like(cb.lower(root.get("assured").get("assuredName")), like),
            cb.like(cb.lower(root.get("loan").get("borrowerName")), like)));
  }

  private static void equal(
      jakarta.persistence.criteria.Path<Object> path,
      String value,
      CriteriaBuilder cb,
      List<Predicate> all) {
    if (value != null && !value.isBlank()) {
      all.add(cb.equal(path, value.strip()));
    }
  }
}
