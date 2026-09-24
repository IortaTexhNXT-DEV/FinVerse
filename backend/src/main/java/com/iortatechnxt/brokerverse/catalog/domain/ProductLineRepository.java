package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Product lines. */
public interface ProductLineRepository extends JpaRepository<ProductLine, Long> {

  /**
   * A line by code.
   *
   * @param code code
   * @return line
   */
  Optional<ProductLine> findByCode(String code);

  /**
   * Every line in display order.
   *
   * @return lines
   */
  List<ProductLine> findAllByOrderBySortOrderAscNameAsc();
}
