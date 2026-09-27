package com.iortatechnxt.brokerverse.nonpackage.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Proposal Request Forms. */
public interface ProposalRequestRepository
    extends JpaRepository<ProposalRequest, Long>, JpaSpecificationExecutor<ProposalRequest> {

  /**
   * PRFs of a client, newest first.
   *
   * @param clientId client
   * @return PRFs
   */
  List<ProposalRequest> findByClientIdOrderByCreatedAtDesc(Long clientId);
}
