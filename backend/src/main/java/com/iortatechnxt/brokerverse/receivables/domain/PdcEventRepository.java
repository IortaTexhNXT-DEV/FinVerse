package com.iortatechnxt.brokerverse.receivables.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link PdcEvent}. */
public interface PdcEventRepository extends JpaRepository<PdcEvent, Long> {

  /**
   * Lists the history of a cheque.
   *
   * @param pdcId cheque
   * @return events in order
   */
  List<PdcEvent> findByPdcIdOrderByIdAsc(Long pdcId);

  /**
   * Lists the history of several cheques (status as of a date in reports).
   *
   * @param pdcIds cheques
   * @return events in order
   */
  List<PdcEvent> findByPdcIdInOrderByIdAsc(Collection<Long> pdcIds);
}
