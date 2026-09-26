package com.iortatechnxt.brokerverse.journal.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Filters of the journal inquiry / transaction checklist. Null values are ignored.
 *
 * @param companyId company (mandatory)
 * @param branchId branch
 * @param status status
 * @param journalType type
 * @param fromDate value date from
 * @param toDate value date to
 * @param batchNo batch number (prefix match)
 * @param inputter maker user name
 * @param authorizer checker user name
 * @param minAmount cut-off amount (total debit at least)
 * @param sourceModule source module
 * @param assignedTo user the entries are assigned to for posting (FRBS 2.5.1)
 */
public record JournalSearchCriteria(
    Long companyId,
    Long branchId,
    JournalStatus status,
    JournalType journalType,
    LocalDate fromDate,
    LocalDate toDate,
    String batchNo,
    String inputter,
    String authorizer,
    BigDecimal minAmount,
    String sourceModule,
    String assignedTo) {

  /**
   * Criteria without an assignee filter.
   *
   * @param companyId company
   * @param branchId branch
   * @param status status
   * @param journalType type
   * @param fromDate value date from
   * @param toDate value date to
   * @param batchNo batch number prefix
   * @param inputter maker
   * @param authorizer checker
   * @param minAmount cut-off amount
   * @param sourceModule source module
   */
  @SuppressWarnings("java:S107") // mirrors the record components
  public JournalSearchCriteria(
      Long companyId,
      Long branchId,
      JournalStatus status,
      JournalType journalType,
      LocalDate fromDate,
      LocalDate toDate,
      String batchNo,
      String inputter,
      String authorizer,
      BigDecimal minAmount,
      String sourceModule) {
    this(
        companyId,
        branchId,
        status,
        journalType,
        fromDate,
        toDate,
        batchNo,
        inputter,
        authorizer,
        minAmount,
        sourceModule,
        null);
  }
}
