package com.iortatechnxt.brokerverse.receivables.service;

import com.iortatechnxt.brokerverse.closing.service.ReconciliationStatusProvider;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Bank reconciliation status for the period-end and year-end checklists: the reconciling items of
 * every bank account of the company up to the period end (book entries not matched to the bank
 * statement and statement lines not matched to the book), as listed by the BRS and the
 * un-reconciled entries reports.
 *
 * <p>Implements the port {@code closing.service.ReconciliationStatusProvider}: receivables depends
 * on closing, never the reverse, so the module graph stays free of cycles.
 */
@Component
public class BankReconciliationStatusProvider implements ReconciliationStatusProvider {

  private final BankReconciliationService reconciliation;

  /**
   * Creates the provider.
   *
   * @param reconciliation bank reconciliation service
   */
  public BankReconciliationStatusProvider(BankReconciliationService reconciliation) {
    this.reconciliation = reconciliation;
  }

  @Override
  public String name() {
    return "Bank reconciliation";
  }

  @Override
  public long unreconciledItems(Long companyId, LocalDate asOf) {
    return reconciliation.unreconciledItems(companyId, asOf);
  }
}
