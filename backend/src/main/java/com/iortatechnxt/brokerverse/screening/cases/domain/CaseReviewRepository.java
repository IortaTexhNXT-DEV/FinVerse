package com.iortatechnxt.brokerverse.screening.cases.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Case reviews (SNSRP-501). */
public interface CaseReviewRepository extends JpaRepository<CaseReview, Long> {

  /**
   * The review of a case.
   *
   * @param caseId case
   * @return the review, empty before it starts
   */
  Optional<CaseReview> findByCaseId(Long caseId);
}
