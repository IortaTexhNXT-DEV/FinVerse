package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Account history views. */
public interface HistoryViewRepository extends JpaRepository<HistoryView, Long> {

  /**
   * Whether a user opened the account history of a candidate.
   *
   * @param candidateId candidate
   * @param createdBy user
   * @return true when viewed
   */
  boolean existsByCandidateIdAndCreatedBy(Long candidateId, String createdBy);

  /**
   * Views of a candidate, newest first.
   *
   * @param candidateId candidate
   * @return views
   */
  List<HistoryView> findByCandidateIdOrderByIdDesc(Long candidateId);
}
