package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insurer allocations of renewal accounts. */
public interface InsurerAllocationRepository extends JpaRepository<InsurerAllocation, Long> {

  /**
   * The allocation of a renewal.
   *
   * @param candidateId renewal
   * @return shares
   */
  List<InsurerAllocation> findByCandidateIdOrderByIdAsc(Long candidateId);

  /**
   * Removes the allocation of a renewal.
   *
   * @param candidateId renewal
   */
  void deleteByCandidateId(Long candidateId);
}
