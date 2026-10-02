package com.iortatechnxt.brokerverse.nonpackage.domain;

import java.util.List;
import java.util.Optional;
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

  /**
   * The record of a renewal on the New Business path (Renewal, BRRN.033).
   *
   * @param renewalRef renewal reference
   * @return the record
   */
  Optional<ProposalRequest> findByRenewalRef(String renewalRef);
}
