package com.iortatechnxt.brokerverse.nbadmin.service;

import java.util.List;

/**
 * Port for data retention (BRNB.106): a module that owns a record type (clients, quotations,
 * proposals, accounts) implements it in its own {@code service} package so the retention review
 * counts and lists eligible records without depending on that module. Implementations only read:
 * physical archiving or purging is parked until the archive store is decided.
 */
public interface RetentionCandidateProvider {

  /**
   * Record type handled, as used in the retention rules (e.g. CLIENT, QUOTATION, ACCOUNT).
   *
   * @return record type
   */
  String recordType();

  /**
   * Number of records in one of the statuses whose last activity is on or before the cutoff.
   *
   * @param criteria statuses and cutoff date
   * @return count
   */
  long countEligible(RetentionCriteria criteria);

  /**
   * Eligible records, oldest activity first.
   *
   * @param criteria statuses and cutoff date
   * @param limit maximum number of records
   * @return records
   */
  List<RetentionCandidate> eligible(RetentionCriteria criteria, int limit);
}
