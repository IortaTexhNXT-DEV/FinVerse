package com.iortatechnxt.finverse.underwriting.domain;

import java.time.LocalDate;

/**
 * Policy search filters; null values are ignored.
 *
 * @param companyId company (mandatory)
 * @param branchId branch
 * @param status status
 * @param productId product
 * @param customerCode client party code
 * @param text policy number or insured name fragment
 * @param fromDate issue date from
 * @param toDate issue date to
 * @param openCoverId open cover (certificates of one cover)
 */
public record PolicySearchCriteria(
    Long companyId,
    Long branchId,
    PolicyStatus status,
    Long productId,
    String customerCode,
    String text,
    LocalDate fromDate,
    LocalDate toDate,
    Long openCoverId) {}
