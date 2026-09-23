package com.iortatechnxt.finverse.reserves.api.dto;

import com.iortatechnxt.finverse.reserves.domain.ReserveParameter;
import com.iortatechnxt.finverse.reserves.domain.ReserveParameterTerms;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Reserve parameter set.
 *
 * @param id id
 * @param companyId company
 * @param businessLine line of business
 * @param effectiveFrom effective date
 * @param ibnrMethod IBNR method
 * @param ibnrRate IBNR rate %
 * @param triangleBasis triangle basis
 * @param developmentPeriod development period
 * @param accidentPeriods accident periods
 * @param mfadPct MfAD %
 * @param ulaePct ULAE %
 * @param expectedLossRatio expected loss ratio %
 * @param treatyCommissionPct treaty RI commission %
 * @param facCommissionPct FAC RI commission %
 * @param remarks remarks
 * @param recordStatus PENDING_AUTHORIZATION, ACTIVE or INACTIVE
 * @param maker last maintainer
 * @param authorizedBy checker
 * @param authorizedAt authorization time
 */
public record ReserveParameterResponse(
    Long id,
    Long companyId,
    String businessLine,
    LocalDate effectiveFrom,
    String ibnrMethod,
    BigDecimal ibnrRate,
    String triangleBasis,
    String developmentPeriod,
    int accidentPeriods,
    BigDecimal mfadPct,
    BigDecimal ulaePct,
    BigDecimal expectedLossRatio,
    BigDecimal treatyCommissionPct,
    BigDecimal facCommissionPct,
    String remarks,
    String recordStatus,
    String maker,
    String authorizedBy,
    Instant authorizedAt) {

  /**
   * Maps a parameter set.
   *
   * @param p parameter set
   * @return response
   */
  public static ReserveParameterResponse from(ReserveParameter p) {
    ReserveParameterTerms t = p.terms();
    return new ReserveParameterResponse(
        p.getId(),
        p.getCompanyId(),
        p.getBusinessLine(),
        p.getEffectiveFrom(),
        t.ibnrMethod().name(),
        t.ibnrRate(),
        t.triangleBasis().name(),
        t.developmentPeriod().name(),
        t.accidentPeriods(),
        t.mfadPct(),
        t.ulaePct(),
        t.expectedLossRatio(),
        t.treatyCommissionPct(),
        t.facCommissionPct(),
        t.remarks(),
        p.getRecordStatus().name(),
        p.getUpdatedBy() != null ? p.getUpdatedBy() : p.getCreatedBy(),
        p.getAuthorizedBy(),
        p.getAuthorizedAt());
  }
}
