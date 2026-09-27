package com.iortatechnxt.brokerverse.reinsurance.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Facultative placements. */
public interface FacPlacementRepository extends JpaRepository<FacPlacement, Long> {

  /**
   * Placements of a company, newest first.
   *
   * @param companyId company
   * @return placements
   */
  List<FacPlacement> findByCompanyIdOrderByIdDesc(Long companyId);

  /**
   * Placements of a company in some statuses, newest first.
   *
   * @param companyId company
   * @param statuses statuses
   * @return placements
   */
  List<FacPlacement> findByCompanyIdAndStatusInOrderByIdDesc(
      Long companyId, Collection<FacStatus> statuses);

  /**
   * Placements of every company in some statuses (approval inbox, alerts).
   *
   * @param statuses statuses
   * @return placements
   */
  List<FacPlacement> findByStatusIn(Collection<FacStatus> statuses);

  /**
   * Placements created by the cessions given.
   *
   * @param cessionIds cessions
   * @return placements
   */
  List<FacPlacement> findByCessionIdIn(Collection<Long> cessionIds);

  /**
   * Placement of a risk in a cession.
   *
   * @param cessionId cession
   * @param riskId risk
   * @return placement
   */
  Optional<FacPlacement> findByCessionIdAndRiskId(Long cessionId, Long riskId);
}
