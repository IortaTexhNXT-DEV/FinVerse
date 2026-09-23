package com.iortatechnxt.finverse.insurance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Outstanding reserve of one open claim at a date, company share, gross of reinsurance.
 *
 * @param companyId company
 * @param branchId branch
 * @param claimId claim id
 * @param claimNo claim number
 * @param policyId policy id
 * @param lineOfBusiness line of business code
 * @param lossDate date of loss
 * @param reportedDate date the claim was notified
 * @param currency claim currency
 * @param outstanding outstanding reserve in the claim currency
 * @param baseOutstanding outstanding reserve in the base currency
 */
public record OutstandingClaim(
    Long companyId,
    Long branchId,
    Long claimId,
    String claimNo,
    Long policyId,
    String lineOfBusiness,
    LocalDate lossDate,
    LocalDate reportedDate,
    String currency,
    BigDecimal outstanding,
    BigDecimal baseOutstanding) {}
