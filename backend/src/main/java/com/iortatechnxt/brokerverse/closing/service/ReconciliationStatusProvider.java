package com.iortatechnxt.brokerverse.closing.service;

import java.time.LocalDate;

/**
 * Port for modules that keep reconciliations (bank, sub-ledger, inter-company…): reports how many
 * items are still unreconciled, for the period-end and year-end checklists.
 *
 * <p>Implement it as a Spring bean in the owning module; the checklists sum all providers. While no
 * provider is registered the checklist shows zero unreconciled items.
 */
public interface ReconciliationStatusProvider {

  /**
   * Name shown in the checklist, e.g. "Bank reconciliation".
   *
   * @return name
   */
  String name();

  /**
   * Counts unreconciled items dated on or before a date.
   *
   * @param companyId company
   * @param asOf date
   * @return number of open items
   */
  long unreconciledItems(Long companyId, LocalDate asOf);
}
