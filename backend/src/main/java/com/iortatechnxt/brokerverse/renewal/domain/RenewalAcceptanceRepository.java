package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Client acceptances. */
public interface RenewalAcceptanceRepository extends JpaRepository<RenewalAcceptance, Long> {

  /**
   * Acceptances of a candidate, newest first.
   *
   * @param candidateId candidate
   * @return acceptances
   */
  List<RenewalAcceptance> findByCandidateIdOrderByIdDesc(Long candidateId);
}
