package com.iortatechnxt.brokerverse.collections.disposition.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Collection efforts. */
public interface EffortRepository extends JpaRepository<Effort, Long> {

  /**
   * Efforts of an item, latest first.
   *
   * @param itemId item
   * @return efforts
   */
  List<Effort> findByItemIdOrderByEffortAtDescIdDesc(Long itemId);
}
