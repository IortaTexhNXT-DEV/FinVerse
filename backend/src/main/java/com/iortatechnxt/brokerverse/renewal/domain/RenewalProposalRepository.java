package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Proposals of the renewal accounts. */
public interface RenewalProposalRepository extends JpaRepository<RenewalProposal, Long> {

  /**
   * The proposals of a renewal, newest first.
   *
   * @param candidateId renewal
   * @return proposals
   */
  List<RenewalProposal> findByCandidateIdOrderByIdDesc(Long candidateId);

  /**
   * A proposal by its number.
   *
   * @param companyId company
   * @param proposalNo number
   * @return proposal
   */
  Optional<RenewalProposal> findByCompanyIdAndProposalNo(Long companyId, String proposalNo);
}
