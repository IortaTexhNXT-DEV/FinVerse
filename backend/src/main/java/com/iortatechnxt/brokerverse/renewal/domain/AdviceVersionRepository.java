package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insurance Advice versions of the renewal accounts. */
public interface AdviceVersionRepository extends JpaRepository<AdviceVersion, Long> {

  /**
   * The versions of a renewal, newest first.
   *
   * @param candidateId renewal
   * @return versions
   */
  List<AdviceVersion> findByCandidateIdOrderByVersionNoDesc(Long candidateId);
}
