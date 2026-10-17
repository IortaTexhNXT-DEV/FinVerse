package com.iortatechnxt.brokerverse.accounting.api.dto;

import com.iortatechnxt.brokerverse.accounting.domain.AccountingEventType;
import com.iortatechnxt.brokerverse.accounting.domain.EventCategory;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;

/**
 * Event type view.
 *
 * @param code code
 * @param name name
 * @param category category
 * @param journalType journal type produced
 * @param description description
 * @param amountComponents components supplied by the publisher
 * @param active active flag
 */
public record EventTypeResponse(
    String code,
    String name,
    EventCategory category,
    JournalType journalType,
    String description,
    String amountComponents,
    boolean active) {

  /**
   * Maps an entity.
   *
   * @param t event type
   * @return response
   */
  public static EventTypeResponse from(AccountingEventType t) {
    return new EventTypeResponse(
        t.getCode(),
        t.getName(),
        t.getCategory(),
        t.getJournalType(),
        t.getDescription(),
        t.getAmountComponents(),
        t.isActive());
  }
}
