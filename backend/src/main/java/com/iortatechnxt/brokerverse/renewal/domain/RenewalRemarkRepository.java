package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Renewal remarks. */
public interface RenewalRemarkRepository extends JpaRepository<RenewalRemark, Long> {

  /**
   * Remarks of a candidate, newest first.
   *
   * @param candidateId candidate
   * @return remarks
   */
  List<RenewalRemark> findByCandidateIdOrderByIdDesc(Long candidateId);
}
