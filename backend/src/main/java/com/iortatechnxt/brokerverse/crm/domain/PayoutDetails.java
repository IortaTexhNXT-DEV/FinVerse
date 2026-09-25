package com.iortatechnxt.brokerverse.crm.domain;

/**
 * CA / SA information as written on a refund request form (MKT 2.25.0, Addendum 1).
 *
 * @param mode credit to account or check
 * @param payeeName account name or check payee name
 * @param accountNo BDO account number (credit to account only)
 */
public record PayoutDetails(PayoutMode mode, String payeeName, String accountNo) {}
