package com.iortatechnxt.brokerverse.nonpackage.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insurer responses. */
public interface InsurerResponseRepository extends JpaRepository<InsurerResponse, Long> {

  /**
   * Responses of a PRF in insurer order.
   *
   * @param proposalId PRF
   * @return responses
   */
  List<InsurerResponse> findByProposalIdOrderById(Long proposalId);

  /**
   * The response of one insurer.
   *
   * @param proposalId PRF
   * @param insurerCode insurer
   * @return response
   */
  Optional<InsurerResponse> findByProposalIdAndInsurerCode(Long proposalId, String insurerCode);
}
