package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Contact Center follow-ups. */
public interface RenewalFollowupRepository extends JpaRepository<RenewalFollowup, Long> {

  /**
   * Follow-ups of a candidate, newest first.
   *
   * @param candidateId candidate
   * @return follow-ups
   */
  List<RenewalFollowup> findByCandidateIdOrderByIdDesc(Long candidateId);
}
