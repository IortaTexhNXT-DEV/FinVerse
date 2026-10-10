package com.iortatechnxt.brokerverse.cashiering.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * An account of a record with the amount paid, cancelled or reinstated for it (FRS.CSH.02.01.04,
 * 04.01.06.01).
 *
 * @param reference account number, invoice number, ARN, policy or PN number
 * @param amount amount for the account
 */
@Embeddable
public record RecordAccount(
    @Column(nullable = false, length = 60) String reference,
    @Column(nullable = false, precision = 19, scale = 2) BigDecimal amount) {}
