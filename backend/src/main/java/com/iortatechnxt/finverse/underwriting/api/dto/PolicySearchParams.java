package com.iortatechnxt.finverse.underwriting.api.dto;

import com.iortatechnxt.finverse.underwriting.domain.PolicySearchCriteria;
import com.iortatechnxt.finverse.underwriting.domain.PolicyStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Query parameters of the policy list, bound from the request URL.
 *
 * @param companyId company (mandatory)
 * @param branchId branch
 * @param status status
 * @param productId product
 * @param customerCode client code
 * @param q policy number or insured name fragment
 * @param fromDate issue date from
 * @param toDate issue date to
 * @param openCoverId open cover (certificates)
 */
public record PolicySearchParams(
    @NotNull Long companyId,
    Long branchId,
    PolicyStatus status,
    Long productId,
    String customerCode,
    String q,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
    Long openCoverId) {

  /**
   * Converts to domain criteria.
   *
   * @return criteria
   */
  public PolicySearchCriteria toCriteria() {
    return new PolicySearchCriteria(
        companyId, branchId, status, productId, customerCode, q, fromDate, toDate, openCoverId);
  }
}
