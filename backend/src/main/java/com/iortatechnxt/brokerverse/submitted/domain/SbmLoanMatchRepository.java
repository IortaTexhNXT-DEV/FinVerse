package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Matching log of the records with the loan files (FR-SP-035). */
public interface SbmLoanMatchRepository extends JpaRepository<SbmLoanMatch, Long> {

  /**
   * The log of a run.
   *
   * @param runId run
   * @param pageable page
   * @return rows
   */
  Page<SbmLoanMatch> findByRunIdOrderByIdAsc(Long runId, Pageable pageable);

  /**
   * The log of a run with one outcome.
   *
   * @param runId run
   * @param outcome outcome
   * @param pageable page
   * @return rows
   */
  Page<SbmLoanMatch> findByRunIdAndOutcomeOrderByIdAsc(
      Long runId, SbmLoanMatch.Outcome outcome, Pageable pageable);

  /**
   * The log of a record, newest first.
   *
   * @param policyId record
   * @return rows
   */
  List<SbmLoanMatch> findByPolicyIdOrderByIdDesc(Long policyId);
}
