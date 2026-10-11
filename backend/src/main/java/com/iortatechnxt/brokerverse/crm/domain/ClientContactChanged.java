package com.iortatechnxt.brokerverse.crm.domain;

import java.util.List;

/**
 * Published inside the transaction when the contact details of a client change through the
 * contact-only contract (BRCSF-004); listeners that call other systems react after the commit.
 *
 * @param companyId company
 * @param clientId client id
 * @param code client or prospect code
 * @param fields changed fields ({@link ContactChange#FIELDS})
 * @param source source of the change, e.g. {@code CSF}
 */
public record ClientContactChanged(
    Long companyId, Long clientId, String code, List<String> fields, String source) {

  /** Defensive copy. */
  public ClientContactChanged {
    fields = List.copyOf(fields);
  }
}
