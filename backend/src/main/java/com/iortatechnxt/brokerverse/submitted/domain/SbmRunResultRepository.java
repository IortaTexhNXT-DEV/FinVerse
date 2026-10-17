package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Results of the processing runs. */
public interface SbmRunResultRepository extends JpaRepository<SbmRunResult, Long> {

  /**
   * Results of a run, optionally of one outcome.
   *
   * @param runId run
   * @param outcome outcome
   * @param pageable page
   * @return results
   */
  Page<SbmRunResult> findByRunIdAndOutcomeOrderByIdAsc(
      Long runId, SbmRunResult.Outcome outcome, Pageable pageable);

  /**
   * Results of a run.
   *
   * @param runId run
   * @param pageable page
   * @return results
   */
  Page<SbmRunResult> findByRunIdOrderByIdAsc(Long runId, Pageable pageable);

  /**
   * Results of a record, newest first.
   *
   * @param policyId record
   * @return results
   */
  List<SbmRunResult> findByPolicyIdOrderByIdDesc(Long policyId);
}
