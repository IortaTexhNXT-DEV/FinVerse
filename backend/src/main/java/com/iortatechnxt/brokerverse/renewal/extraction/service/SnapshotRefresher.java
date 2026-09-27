package com.iortatechnxt.brokerverse.renewal.extraction.service;

import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import org.springframework.stereotype.Component;

/**
 * Refreshes the listing snapshot of a booked renewal from the current account and ledger at each
 * evaluation (BRRN.011: policy fields are read from the current account and invoice, so an
 * endorsement on the mother policy shows without a new extraction). Migrated and submitted policies
 * keep the snapshot of their header.
 */
@Component
public class SnapshotRefresher {

  private final ExpiringPolicies policies;
  private final CandidateFactory factory;

  /**
   * Creates the refresher.
   *
   * @param policies expiring invoices
   * @param factory snapshot builder
   */
  public SnapshotRefresher(ExpiringPolicies policies, CandidateFactory factory) {
    this.policies = policies;
    this.factory = factory;
  }

  /**
   * Refreshes the snapshot of a renewal.
   *
   * @param candidate candidate
   */
  public void refresh(RenewalCandidate candidate) {
    if (candidate.getSource() != CandidateSource.BIBS_INVOICE
        || candidate.getExpiringInvoiceNo() == null) {
      return;
    }
    policies
        .invoice(candidate.getCompanyId(), candidate.getExpiringInvoiceNo())
        .ifPresent(i -> candidate.refresh(factory.snapshot(candidate.getCompanyId(), i)));
  }
}
