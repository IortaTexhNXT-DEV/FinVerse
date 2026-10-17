package com.iortatechnxt.brokerverse.collections.common.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Field-level change log of Collections (BRCLXN.043). */
public interface FieldChangeRepository extends JpaRepository<FieldChange, Long> {

  /**
   * Changes of an item, newest first.
   *
   * @param itemId item
   * @param pageable page
   * @return changes
   */
  Page<FieldChange> findByItemIdOrderByIdDesc(Long itemId, Pageable pageable);
}
