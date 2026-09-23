package com.iortatechnxt.finverse.claims.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Values of a new claim movement line.
 *
 * @param claim claim
 * @param kind estimate change or paid amount
 * @param line side, cost type and signed amount at 100 %
 * @param amount signed company share
 * @param baseAmount signed company share in base currency
 * @param date movement (accounting) date
 * @param source producing document type
 * @param sourceId producing document id
 * @param reference unique, stable reference (idempotency key for listeners)
 * @param journalBatchNo journal that posted the movement
 * @param narration narration
 */
public record MovementValues(
    Claim claim,
    MovementKind kind,
    EstimateLine line,
    BigDecimal amount,
    BigDecimal baseAmount,
    LocalDate date,
    MovementSource source,
    Long sourceId,
    String reference,
    String journalBatchNo,
    String narration) {}
