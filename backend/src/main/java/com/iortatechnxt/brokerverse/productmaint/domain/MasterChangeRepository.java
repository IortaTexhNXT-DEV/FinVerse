package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Product master changes. */
public interface MasterChangeRepository extends JpaRepository<MasterChange, Long> {

  /**
   * Changes in a status, oldest first.
   *
   * @param status status
   * @return changes
   */
  List<MasterChange> findByStatusOrderByIdAsc(String status);

  /**
   * Changes in a status, newest first.
   *
   * @param status status
   * @param pageable page
   * @return page
   */
  Page<MasterChange> findByStatus(String status, Pageable pageable);

  /**
   * Number of changes in a status (monitoring).
   *
   * @param status status
   * @return count
   */
  long countByStatus(String status);
}
