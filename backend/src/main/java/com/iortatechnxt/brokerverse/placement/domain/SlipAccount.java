package com.iortatechnxt.brokerverse.placement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * An account placed on a slip.
 *
 * @param accountId account id
 * @param arn Account Reference Number
 */
@Embeddable
public record SlipAccount(
    @Column(name = "account_id", nullable = false) Long accountId,
    @Column(name = "arn", nullable = false, length = 30) String arn) {}
