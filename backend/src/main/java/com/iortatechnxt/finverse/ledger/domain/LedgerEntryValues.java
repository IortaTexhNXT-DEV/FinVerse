package com.iortatechnxt.finverse.ledger.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Values of a ledger entry (parameter object for the posting engine).
 *
 * @param companyId company
 * @param branchId branch
 * @param accountId account
 * @param periodId period
 * @param valueDate value date
 * @param batchId journal batch
 * @param batchNo batch number
 * @param lineNo line number
 * @param journalType journal type
 * @param currency transaction currency
 * @param debitFc debit in transaction currency
 * @param creditFc credit in transaction currency
 * @param debitBase debit in base currency
 * @param creditBase credit in base currency
 * @param costCenter cost centre
 * @param businessLine line of business
 * @param partyCode sub-ledger party
 * @param reference reference
 * @param narration narration
 * @param postedAt posting time
 * @param postedBy posting user (authorizer)
 */
public record LedgerEntryValues(
    Long companyId,
    Long branchId,
    Long accountId,
    Long periodId,
    LocalDate valueDate,
    Long batchId,
    String batchNo,
    int lineNo,
    String journalType,
    String currency,
    BigDecimal debitFc,
    BigDecimal creditFc,
    BigDecimal debitBase,
    BigDecimal creditBase,
    String costCenter,
    String businessLine,
    String partyCode,
    String reference,
    String narration,
    Instant postedAt,
    String postedBy) {}
