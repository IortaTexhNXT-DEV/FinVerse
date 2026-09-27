package com.iortatechnxt.brokerverse.eb.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Comparatives. */
public interface EbComparativeRepository extends JpaRepository<EbComparative, Long> {

  /**
   * Versions of a cycle.
   *
   * @param cycleId cycle
   * @return versions, latest first
   */
  List<EbComparative> findByCycleIdOrderByVersionNoDesc(Long cycleId);

  /**
   * Versions of a programme.
   *
   * @param programmeId programme
   * @return versions, latest first
   */
  List<EbComparative> findByProgrammeIdOrderByIdDesc(Long programmeId);

  /**
   * A comparative of a company.
   *
   * @param id comparative
   * @param companyId company
   * @return comparative
   */
  Optional<EbComparative> findByIdAndCompanyId(Long id, Long companyId);

  /**
   * Comparatives in given statuses (approval inbox).
   *
   * @param statuses statuses
   * @return comparatives
   */
  List<EbComparative> findByStatusIn(Collection<EbComparative.Status> statuses);
}
