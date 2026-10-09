package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Renewal proposals (FR-SP-066, 067). */
public interface SbmProposalRepository extends JpaRepository<SbmProposal, Long> {

  /**
   * The proposals of a batch.
   *
   * @param batchId batch
   * @return proposals
   */
  List<SbmProposal> findByBatchIdOrderByIdAsc(Long batchId);

  /**
   * The proposals of a record, newest version first.
   *
   * @param policyId record
   * @return proposals
   */
  List<SbmProposal> findByPolicyIdOrderByVersionNoDesc(Long policyId);

  /**
   * Proposals in a status (Team Lead's inbox).
   *
   * @param status status
   * @return proposals
   */
  List<SbmProposal> findByStatus(SbmProposal.Status status);
}
