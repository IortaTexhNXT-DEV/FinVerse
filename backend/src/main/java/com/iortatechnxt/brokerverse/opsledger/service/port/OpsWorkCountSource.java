package com.iortatechnxt.brokerverse.opsledger.service.port;

import java.util.List;

/**
 * Port: the work counts a module shows on the Operations home (BRQID.003): one tile per queue,
 * grouped by team section. The ledger provides the counts it can derive from invoices; each
 * Operations module adds its own queues (payments to match, batches for approval, ...) as a Spring
 * bean. The home shows a section only to users holding one of its permissions.
 */
public interface OpsWorkCountSource {

  /**
   * The module's counts for a company.
   *
   * @param companyId company
   * @return tiles
   */
  List<WorkCount> counts(Long companyId);

  /** Team sections of the Operations home, in display order. */
  enum Section {
    /** Cashiering. */
    CASHIERING,
    /** Remittance, holds and special remittance. */
    REMITTANCE,
    /** Production reconciliation. */
    PRODRECON,
    /** Adjustment / cancellation. */
    ADJUSTMENT,
    /** Commission receivables (direct payment) and incentives. */
    COMMISSION,
    /** Disbursement queue. */
    DISBURSEMENT,
    /** Interfaces with other systems. */
    INTERFACES
  }

  /** How a count should draw attention. */
  enum Severity {
    /** Informational. */
    INFO,
    /** Needs attention soon (amber). */
    WARNING,
    /** Overdue or failed (red). */
    ALERT
  }

  /**
   * One tile.
   *
   * @param section team section
   * @param key unique key within the section
   * @param label tile label
   * @param count count
   * @param severity attention level when the count is not zero
   * @param link frontend route of the list behind the tile
   */
  record WorkCount(
      Section section, String key, String label, long count, Severity severity, String link) {}
}
