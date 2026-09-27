package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Cover types. */
public interface CoverTypeRepository extends JpaRepository<CoverType, Long> {

  /**
   * A cover type of a line.
   *
   * @param lineCode line
   * @param code code
   * @return cover type
   */
  Optional<CoverType> findByLineCodeAndCode(String lineCode, String code);

  /**
   * Every cover type by line and order.
   *
   * @return cover types
   */
  List<CoverType> findAllByOrderByLineCodeAscSortOrderAscNameAsc();
}
