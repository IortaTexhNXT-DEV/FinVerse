package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Coverages and perils (PMADD01). */
public interface CoverageRepository extends JpaRepository<Coverage, Long> {

  /**
   * A coverage of a line.
   *
   * @param lineCode line
   * @param code code
   * @return coverage
   */
  Optional<Coverage> findByLineCodeAndCode(String lineCode, String code);

  /**
   * Every coverage by line and order.
   *
   * @return coverages
   */
  List<Coverage> findAllByOrderByLineCodeAscSortOrderAscCodeAsc();

  /**
   * The coverages of a line in order.
   *
   * @param lineCode line
   * @return coverages
   */
  List<Coverage> findByLineCodeOrderBySortOrderAscCodeAsc(String lineCode);
}
