package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.AccountStatus;

/**
 * Published synchronously, inside the transaction, after an account's status changed (any user,
 * system or generic workflow action). Later modules listen to it rather than to the generic {@code
 * WorkCaseTransitioned} when they need the ARN and account-level status.
 *
 * @param accountId account
 * @param arn Account Reference Number
 * @param from previous status
 * @param to new status
 * @param action workflow action
 * @param reasonCode reason
 * @param comment comment
 */
public record AccountStatusChanged(
    Long accountId,
    String arn,
    AccountStatus from,
    AccountStatus to,
    String action,
    String reasonCode,
    String comment) {}
