package com.iortatechnxt.brokerverse.nonpackage.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Version history of insurer responses. */
public interface InsurerResponseHistoryRepository
    extends JpaRepository<InsurerResponseHistory, Long> {

  /**
   * History of the responses of a PRF, newest first.
   *
   * @param responseIds responses
   * @return snapshots
   */
  List<InsurerResponseHistory> findByResponseIdInOrderByIdDesc(List<Long> responseIds);
}
