package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Policy reviews. */
public interface SbmIaafReviewRepository extends JpaRepository<SbmIaafReview, Long> {

  /**
   * Reviews of a policy, oldest first.
   *
   * @param policyId policy
   * @return reviews
   */
  List<SbmIaafReview> findByPolicyIdOrderByReviewNoAsc(Long policyId);
}
