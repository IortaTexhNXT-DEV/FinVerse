package com.iortatechnxt.brokerverse.payables.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link IssuedPdcEvent}. */
public interface IssuedPdcEventRepository extends JpaRepository<IssuedPdcEvent, Long> {

  /**
   * History of a cheque.
   *
   * @param pdcId cheque
   * @return events in order
   */
  List<IssuedPdcEvent> findByPdcIdOrderByIdAsc(Long pdcId);
}
