package com.iortatechnxt.brokerverse.claims.service;

import com.iortatechnxt.brokerverse.claims.domain.RecoveryType;
import java.math.BigDecimal;

/**
 * Maker input of a recovery, at 100 %.
 *
 * @param recoveryType salvage or subrogation
 * @param fromPartyCode payer party code, may be null
 * @param bankAccountCode GL bank account that received the money
 * @param amount amount received
 * @param narration narration
 */
public record RecoveryCommand(
    RecoveryType recoveryType,
    String fromPartyCode,
    String bankAccountCode,
    BigDecimal amount,
    String narration) {}
