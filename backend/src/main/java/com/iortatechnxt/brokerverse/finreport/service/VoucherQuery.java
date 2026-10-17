package com.iortatechnxt.brokerverse.finreport.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Selection of journal lines for voucher based reports.
 *
 * @param companyId company
 * @param branchId voucher branch or null for all
 * @param from first document (value) date
 * @param to last document date
 * @param status posted / unposted selection
 * @param journalTypes journal type names to include (from the transaction code range)
 * @param docFrom lowest voucher number or null
 * @param docTo highest voucher number or null
 * @param user entered-by user or null
 * @param accountIds accounts whose lines are included
 */
public record VoucherQuery(
    Long companyId,
    Long branchId,
    LocalDate from,
    LocalDate to,
    StatusFilter status,
    Collection<String> journalTypes,
    String docFrom,
    String docTo,
    String user,
    Collection<Long> accountIds) {

  /** Canonical constructor copying collections. */
  public VoucherQuery {
    journalTypes = List.copyOf(journalTypes);
    accountIds = List.copyOf(accountIds);
  }
}
