package com.iortatechnxt.brokerverse.csf.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Legacy contact sync outbox. */
public interface CsfSyncOutboxRepository extends JpaRepository<CsfSyncOutbox, Long> {

  /**
   * Rows in given states, oldest first.
   *
   * @param statuses states
   * @return rows
   */
  List<CsfSyncOutbox> findByStatusInOrderByIdAsc(Collection<OutboxStatus> statuses);

  /**
   * Rows of a change.
   *
   * @param changeId change
   * @return rows
   */
  List<CsfSyncOutbox> findByChangeIdOrderByIdAsc(Long changeId);
}
