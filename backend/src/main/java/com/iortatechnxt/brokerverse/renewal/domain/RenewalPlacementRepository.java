package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Placements of the renewal accounts. */
public interface RenewalPlacementRepository extends JpaRepository<RenewalPlacement, Long> {

  /**
   * The placements of a renewal, newest first.
   *
   * @param candidateId renewal
   * @return placements
   */
  List<RenewalPlacement> findByCandidateIdOrderByIdDesc(Long candidateId);

  /**
   * The placements of a placement file.
   *
   * @param fileId placement file
   * @return placements
   */
  List<RenewalPlacement> findByPlacementFileId(Long fileId);

  /**
   * The placements sent through channel messages.
   *
   * @param messageNos messages
   * @return placements
   */
  List<RenewalPlacement> findByMessageNoIn(Collection<String> messageNos);
}
