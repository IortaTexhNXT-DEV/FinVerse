package com.iortatechnxt.brokerverse.renewal.candidate.service;

import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Tab;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.EnumSet;
import java.util.Set;

/** JPA filters of the bucket panels of the renewal accounts (FRRN.002.05). */
final class BucketPanelSpecifications {

  private static final String DISPOSITION = "disposition";
  private static final String CODE = "code";

  private BucketPanelSpecifications() {}

  /** The bucket panels of the renewal accounts (FRRN.002.05); every renewal otherwise. */
  static Predicate of(Root<RenewalCandidate> root, CriteriaBuilder cb, Tab tab) {
    return switch (tab) {
      case BUCKET_CLEAN -> panel(root, cb, EnumSet.of(Bucket.CLEAN));
      case BUCKET_REVIEW -> panel(root, cb, EnumSet.of(Bucket.REVIEW, Bucket.EXCEPTION));
      case BUCKET_REVIEW_ONLY -> panel(root, cb, EnumSet.of(Bucket.REVIEW));
      case BUCKET_NON_RENEWABLE ->
          cb.equal(root.get(DISPOSITION).get(CODE), RenewalDisposition.NOT_FOR_RENEWAL);
      default -> cb.conjunction();
    };
  }

  /** A bucket panel: the buckets given, without the Not for Renewal accounts. */
  private static Predicate panel(
      Root<RenewalCandidate> root, CriteriaBuilder cb, Set<Bucket> buckets) {
    Path<Object> disposition = root.get(DISPOSITION).get(CODE);
    return cb.and(
        root.get("bucket").in(buckets),
        cb.or(
            cb.isNull(disposition), cb.notEqual(disposition, RenewalDisposition.NOT_FOR_RENEWAL)));
  }
}
