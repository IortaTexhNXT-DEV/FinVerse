package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Check runs. */
public interface CheckRunRepository extends JpaRepository<CheckRun, Long> {

  /**
   * Runs of a candidate, newest first.
   *
   * @param candidateId candidate
   * @return runs
   */
  List<CheckRun> findByCandidateIdOrderByRunAtDescIdDesc(Long candidateId);
}
