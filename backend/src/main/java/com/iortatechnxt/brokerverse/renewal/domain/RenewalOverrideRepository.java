package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Controlled overrides. */
public interface RenewalOverrideRepository extends JpaRepository<RenewalOverride, Long> {

  /**
   * Overrides of a candidate, newest first.
   *
   * @param candidateId candidate
   * @return overrides
   */
  List<RenewalOverride> findByCandidateIdOrderByIdDesc(Long candidateId);

  /**
   * Active overrides of a candidate.
   *
   * @param candidateId candidate
   * @return overrides
   */
  List<RenewalOverride> findByCandidateIdAndActiveTrue(Long candidateId);
}
