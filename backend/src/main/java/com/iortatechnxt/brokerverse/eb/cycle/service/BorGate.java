package com.iortatechnxt.brokerverse.eb.cycle.service;

import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import java.time.LocalDate;

/**
 * Port: refuses a step of a cycle while the programme has no validated Broker on Record in force
 * (BRID-008; FR-EB-031 R1: insurer and franchise requests of a new business or a remarketing).
 * Implemented by the BOR service.
 */
public interface BorGate {

  /**
   * Refuses when no validated BOR of the programme is in force on the date.
   *
   * @param cycle cycle
   * @param date business date
   */
  void requireValidated(EbCycle cycle, LocalDate date);
}
