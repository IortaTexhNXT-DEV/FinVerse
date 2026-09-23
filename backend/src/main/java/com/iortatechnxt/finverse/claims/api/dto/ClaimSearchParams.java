package com.iortatechnxt.finverse.claims.api.dto;

import com.iortatechnxt.finverse.claims.domain.ClaimSearchCriteria;
import com.iortatechnxt.finverse.claims.domain.ClaimStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Query parameters of the claim list.
 *
 * @param companyId company (mandatory)
 * @param branchId branch
 * @param status status
 * @param businessLine class
 * @param q claim number, policy number or insured fragment
 * @param lossFrom date of loss from
 * @param lossTo date of loss to
 * @param policyId claims of one policy
 */
public record ClaimSearchParams(
    @NotNull Long companyId,
    Long branchId,
    ClaimStatus status,
    String businessLine,
    String q,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lossFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lossTo,
    Long policyId) {

  /**
   * Converts to domain criteria.
   *
   * @return criteria
   */
  public ClaimSearchCriteria toCriteria() {
    return new ClaimSearchCriteria(
        companyId, branchId, status, businessLine, q, lossFrom, lossTo, policyId);
  }
}
