package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Letters. */
public interface SbmLetterRepository extends JpaRepository<SbmLetter, Long> {

  /**
   * Whether a rule already produced a letter for a record.
   *
   * @param policyId record
   * @param ruleId rule
   * @return true when it did
   */
  boolean existsByPolicyIdAndRuleId(Long policyId, Long ruleId);

  /**
   * Letters of a record, newest first.
   *
   * @param policyId record
   * @return letters
   */
  List<SbmLetter> findByPolicyIdOrderByIdDesc(Long policyId);

  /**
   * Letters in some statuses.
   *
   * @param companyId company
   * @param statuses statuses
   * @param pageable page
   * @return letters, newest first
   */
  Page<SbmLetter> findByCompanyIdAndStatusInOrderByIdDesc(
      Long companyId, Collection<String> statuses, Pageable pageable);

  /**
   * Letters of a channel and status (print batch).
   *
   * @param companyId company
   * @param channel PRINT
   * @param status GENERATED
   * @return letters
   */
  List<SbmLetter> findByCompanyIdAndChannelAndStatusOrderByIdAsc(
      Long companyId, String channel, String status);

  /**
   * Letters of a print batch.
   *
   * @param printBatchId batch
   * @return letters
   */
  List<SbmLetter> findByPrintBatchIdOrderByIdAsc(Long printBatchId);

  /**
   * Count of a company in a status (home).
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, String status);
}
