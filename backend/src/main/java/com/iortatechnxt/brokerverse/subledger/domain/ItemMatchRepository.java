package com.iortatechnxt.brokerverse.subledger.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link ItemMatch}. */
public interface ItemMatchRepository extends JpaRepository<ItemMatch, Long> {

  /**
   * Lists matches involving any of the given debit items.
   *
   * @param ids debit item ids
   * @return matches
   */
  List<ItemMatch> findByDebitItemIdIn(Collection<Long> ids);

  /**
   * Lists matches involving any of the given credit items.
   *
   * @param ids credit item ids
   * @return matches
   */
  List<ItemMatch> findByCreditItemIdIn(Collection<Long> ids);
}
