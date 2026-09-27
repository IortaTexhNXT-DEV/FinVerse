package com.iortatechnxt.brokerverse.eb.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insurer proposals. */
public interface EbProposalRepository extends JpaRepository<EbProposal, Long> {

  /**
   * Proposals of a cycle.
   *
   * @param cycleId cycle
   * @return proposals, oldest first
   */
  List<EbProposal> findByCycleIdOrderByIdAsc(Long cycleId);

  /**
   * Proposals of a cycle in given statuses.
   *
   * @param cycleId cycle
   * @param statuses statuses
   * @return proposals, oldest first
   */
  List<EbProposal> findByCycleIdAndStatusInOrderByIdAsc(
      Long cycleId, Collection<EbProposal.Status> statuses);

  /**
   * The latest version of an insurer on a cycle.
   *
   * @param cycleId cycle
   * @param insurerCode insurer
   * @return proposal
   */
  Optional<EbProposal> findFirstByCycleIdAndInsurerCodeOrderByVersionNoDesc(
      Long cycleId, String insurerCode);

  /**
   * A proposal of a company.
   *
   * @param id proposal
   * @param companyId company
   * @return proposal
   */
  Optional<EbProposal> findByIdAndCompanyId(Long id, Long companyId);
}
