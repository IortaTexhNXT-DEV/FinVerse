package com.iortatechnxt.finverse.payables.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link PdcEvent}. */
public interface PdcEventRepository extends JpaRepository<PdcEvent, Long> {

  /**
   * History of a cheque.
   *
   * @param pdcId cheque
   * @return events in order
   */
  List<PdcEvent> findByPdcIdOrderByIdAsc(Long pdcId);
}
