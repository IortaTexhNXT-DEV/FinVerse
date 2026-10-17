package com.iortatechnxt.brokerverse.screening.cases.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Review answers (SNSRP-501). */
public interface CaseAnswerRepository extends JpaRepository<CaseAnswer, Long> {

  /**
   * The answers of a review.
   *
   * @param reviewId review
   * @return answers
   */
  List<CaseAnswer> findByReviewId(Long reviewId);
}
