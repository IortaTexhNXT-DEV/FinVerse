package com.iortatechnxt.brokerverse.migration.mapping.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Columns of the layout versions. */
public interface LayoutColumnRepository extends JpaRepository<LayoutColumn, Long> {

  /**
   * The columns of a layout version in file order.
   *
   * @param layoutId layout version
   * @return columns
   */
  List<LayoutColumn> findByLayoutIdOrderBySeqAsc(Long layoutId);
}
