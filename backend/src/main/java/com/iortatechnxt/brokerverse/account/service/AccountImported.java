package com.iortatechnxt.brokerverse.account.service;

/**
 * Published inside the transaction when the data migration imports a legacy policy as an account
 * (instead of {@link AccountStatusChanged}, so no status notification is sent for a migrated
 * account). account never depends on its listeners.
 *
 * @param companyId company
 * @param accountId account
 * @param arn ARN
 * @param legacyRef legacy policy reference
 */
public record AccountImported(Long companyId, Long accountId, String arn, String legacyRef) {}
