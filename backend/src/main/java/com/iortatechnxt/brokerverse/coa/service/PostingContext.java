package com.iortatechnxt.brokerverse.coa.service;

import java.time.LocalDate;
import java.util.Set;

/**
 * Facts about a proposed posting line, used to check account eligibility.
 *
 * @param branchId posting branch
 * @param currency transaction currency
 * @param valueDate value date
 * @param manual true for manual journals (false for system generated)
 * @param roleCodes role codes of the maker
 * @param hasCostCenter whether a cost centre was supplied
 * @param hasBusinessLine whether a line of business was supplied
 * @param hasSubLedgerParty whether a sub-ledger party reference was supplied
 */
public record PostingContext(
    Long branchId,
    String currency,
    LocalDate valueDate,
    boolean manual,
    Set<String> roleCodes,
    boolean hasCostCenter,
    boolean hasBusinessLine,
    boolean hasSubLedgerParty) {}
