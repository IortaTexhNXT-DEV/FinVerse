package com.iortatechnxt.finverse.claims.domain;

import java.time.LocalDate;

/**
 * Filters of the claim list; null = all values.
 *
 * @param companyId company (mandatory)
 * @param branchId branch
 * @param status status
 * @param businessLine class
 * @param q claim number, policy number or insured name fragment
 * @param lossFrom date of loss from
 * @param lossTo date of loss to
 * @param policyId claims of one policy
 */
public record ClaimSearchCriteria(
    Long companyId,
    Long branchId,
    ClaimStatus status,
    String businessLine,
    String q,
    LocalDate lossFrom,
    LocalDate lossTo,
    Long policyId) {}
