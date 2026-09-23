package com.iortatechnxt.finverse.insurance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One posted financial movement of a claim, company share (after coinsurance), gross of
 * reinsurance.
 *
 * @param companyId company
 * @param branchId branch that owns the claim
 * @param claimId claim id
 * @param claimNo claim number
 * @param policyId policy the claim is made under
 * @param lineOfBusiness line of business code of the policy product
 * @param lossDate date of loss (selects the reinsurance programme and the accident year)
 * @param movementDate accounting date of the movement
 * @param type movement type
 * @param currency claim currency
 * @param amount amount in the claim currency (reserve changes are signed deltas)
 * @param baseAmount amount in the company base currency at the posting rate
 * @param reference unique, stable reference of the movement (idempotency key for listeners)
 */
public record ClaimMovement(
    Long companyId,
    Long branchId,
    Long claimId,
    String claimNo,
    Long policyId,
    String lineOfBusiness,
    LocalDate lossDate,
    LocalDate movementDate,
    ClaimMovementType type,
    String currency,
    BigDecimal amount,
    BigDecimal baseAmount,
    String reference) {}
