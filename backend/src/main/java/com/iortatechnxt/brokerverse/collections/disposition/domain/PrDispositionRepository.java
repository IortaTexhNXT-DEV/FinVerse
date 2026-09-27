package com.iortatechnxt.brokerverse.collections.disposition.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** PR collector dispositions (BRCLXN.016-023). */
public interface PrDispositionRepository extends JpaRepository<PrDisposition, Long> {

  /**
   * Dispositions of an item, newest first.
   *
   * @param itemId item
   * @return dispositions
   */
  List<PrDisposition> findByItemIdOrderByIdDesc(Long itemId);
}
