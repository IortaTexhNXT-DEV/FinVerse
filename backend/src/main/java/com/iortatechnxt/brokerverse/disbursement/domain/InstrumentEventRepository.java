package com.iortatechnxt.brokerverse.disbursement.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Status history of instruments (DIS 2.8.0, 3.26.0). */
public interface InstrumentEventRepository extends JpaRepository<InstrumentEvent, Long> {

  /**
   * The history of an instrument, oldest first.
   *
   * @param instrumentId instrument
   * @return events
   */
  List<InstrumentEvent> findByInstrumentIdOrderByIdAsc(Long instrumentId);
}
