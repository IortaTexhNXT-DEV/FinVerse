package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Check results. */
public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {

  /**
   * Results of a run.
   *
   * @param runId run
   * @return results
   */
  List<CheckResult> findByRunIdOrderByIdAsc(Long runId);

  /**
   * Results of some checks in several runs (list columns of a chunk).
   *
   * @param runIds runs
   * @param checkCodes checks
   * @return results
   */
  List<CheckResult> findByRunIdInAndCheckCodeIn(
      Collection<Long> runIds, Collection<String> checkCodes);
}
