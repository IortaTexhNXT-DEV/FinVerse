package com.iortatechnxt.finverse.underwriting.service;

import com.iortatechnxt.finverse.underwriting.domain.DateBasis;
import com.iortatechnxt.finverse.underwriting.domain.PolicyStatus;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/**
 * Selection of premium transactions.
 *
 * @param companyId company
 * @param basis date that selects the transactions
 * @param from from date, null for open
 * @param to to date, null for open
 * @param statuses document statuses to include
 */
public record TransactionQuery(
    Long companyId, DateBasis basis, LocalDate from, LocalDate to, Set<PolicyStatus> statuses) {

  /** Canonical constructor copying the statuses. */
  public TransactionQuery {
    statuses = Set.copyOf(statuses);
  }

  /**
   * Approved transactions (including those of since-cancelled policies) by approval date.
   *
   * @param companyId company
   * @param from approval date from
   * @param to approval date to
   * @return query
   */
  public static TransactionQuery approved(Long companyId, LocalDate from, LocalDate to) {
    return new TransactionQuery(
        companyId,
        DateBasis.APPROVAL,
        from,
        to,
        EnumSet.of(PolicyStatus.APPROVED, PolicyStatus.CANCELLED));
  }
}
