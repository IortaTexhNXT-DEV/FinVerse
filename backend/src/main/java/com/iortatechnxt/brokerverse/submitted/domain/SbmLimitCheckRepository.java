package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Limit checks. */
public interface SbmLimitCheckRepository extends JpaRepository<SbmLimitCheck, Long> {

  /**
   * Checks of a record, newest first.
   *
   * @param policyId record
   * @return checks
   */
  List<SbmLimitCheck> findByPolicyIdOrderByIdDesc(Long policyId);

  /**
   * Checks of a record in a run.
   *
   * @param policyId record
   * @param runId run
   * @return checks
   */
  List<SbmLimitCheck> findByPolicyIdAndRunIdOrderByIdAsc(Long policyId, Long runId);
}
