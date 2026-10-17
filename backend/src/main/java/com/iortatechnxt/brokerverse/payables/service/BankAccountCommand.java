package com.iortatechnxt.brokerverse.payables.service;

import com.iortatechnxt.brokerverse.payables.domain.NotificationFormat;

/**
 * Values to create or update a bank account.
 *
 * @param companyId company
 * @param code code (immutable)
 * @param name name
 * @param bankPartyCode bank party code (optional)
 * @param bankName bank name
 * @param accountNo account number
 * @param currency currency
 * @param glAccountCode bank GL account (postable, bank category)
 * @param pdcClearingAccountCode PDC-issued clearing account (optional)
 * @param branchId owning branch (optional)
 * @param notificationFormat payment notification layout
 */
public record BankAccountCommand(
    Long companyId,
    String code,
    String name,
    String bankPartyCode,
    String bankName,
    String accountNo,
    String currency,
    String glAccountCode,
    String pdcClearingAccountCode,
    Long branchId,
    NotificationFormat notificationFormat) {}
