package com.iortatechnxt.brokerverse.crm.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** History of client tags and instructions. */
public interface NoteHistoryRepository extends JpaRepository<NoteHistory, Long> {

  /**
   * History of a client, newest first.
   *
   * @param clientId client
   * @return changes
   */
  List<NoteHistory> findByClientIdOrderByIdDesc(Long clientId);
}
