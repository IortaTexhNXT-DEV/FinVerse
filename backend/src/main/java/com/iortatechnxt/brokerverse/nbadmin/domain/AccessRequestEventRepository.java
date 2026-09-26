package com.iortatechnxt.brokerverse.nbadmin.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** History of the access requests (insert-only). */
public interface AccessRequestEventRepository extends JpaRepository<AccessRequestEvent, Long> {

  /**
   * History of one request, oldest first.
   *
   * @param requestId request
   * @return events
   */
  List<AccessRequestEvent> findByRequestIdOrderByIdAsc(Long requestId);
}
