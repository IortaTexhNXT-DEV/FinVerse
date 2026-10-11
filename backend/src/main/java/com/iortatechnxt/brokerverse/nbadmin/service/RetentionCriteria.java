package com.iortatechnxt.brokerverse.nbadmin.service;

import java.time.LocalDate;
import java.util.Set;

/**
 * Which records a retention rule selects.
 *
 * @param statuses record statuses (e.g. NOT_PROCEEDED, VOIDED, INACTIVE)
 * @param lastActivityOnOrBefore cutoff: business date minus the years online
 */
public record RetentionCriteria(Set<String> statuses, LocalDate lastActivityOnOrBefore) {

  /** Defensive copy. */
  public RetentionCriteria {
    statuses = Set.copyOf(statuses);
  }
}
