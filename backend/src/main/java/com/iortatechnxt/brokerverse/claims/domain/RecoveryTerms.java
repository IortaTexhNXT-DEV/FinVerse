package com.iortatechnxt.brokerverse.claims.domain;

import com.iortatechnxt.brokerverse.party.domain.Party;
import java.math.BigDecimal;

/**
 * Terms of a new recovery.
 *
 * @param recoveryType salvage or subrogation
 * @param fromParty payer (salvage buyer, third party or its insurer), may be null
 * @param bankAccountCode GL bank account that received the money
 * @param amount amount received at 100 %
 * @param narration narration
 */
public record RecoveryTerms(
    RecoveryType recoveryType,
    Party fromParty,
    String bankAccountCode,
    BigDecimal amount,
    String narration) {}
