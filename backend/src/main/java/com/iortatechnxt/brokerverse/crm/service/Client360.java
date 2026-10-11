package com.iortatechnxt.brokerverse.crm.service;

import java.util.List;

/**
 * Client 360 view (BRNB.099): records linked to the client across modules and missing or broken
 * linkages.
 *
 * @param records linked records, newest first
 * @param warnings linkage and data warnings, e.g. a confirmed client without a party
 */
public record Client360(List<ClientRecord> records, List<Warning> warnings) {

  /** Defensive copies. */
  public Client360 {
    records = List.copyOf(records);
    warnings = List.copyOf(warnings);
  }

  /**
   * A flagged gap.
   *
   * @param code stable code, e.g. MISSING_PARTY
   * @param message readable message
   */
  public record Warning(String code, String message) {}
}
