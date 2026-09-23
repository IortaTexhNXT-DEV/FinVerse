package com.iortatechnxt.finverse.reserves.api.dto;

import com.iortatechnxt.finverse.reserves.domain.DevelopmentPeriod;
import com.iortatechnxt.finverse.reserves.domain.IbnrMethod;
import com.iortatechnxt.finverse.reserves.domain.ReserveParameterTerms;
import com.iortatechnxt.finverse.reserves.domain.TriangleBasis;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create / update reserve parameters. {@code companyId}, {@code businessLine} and {@code
 * effectiveFrom} identify the record and are ignored on update. Percentages as numbers (12.5 = 12.5
 * %).
 *
 * @param companyId company
 * @param businessLine line of business
 * @param effectiveFrom first valuation date the values apply to
 * @param ibnrMethod RATE or CHAIN_LADDER
 * @param ibnrRate IBNR rate % of earned premium (rate method)
 * @param triangleBasis PAID or INCURRED (chain-ladder)
 * @param developmentPeriod YEAR or QUARTER (chain-ladder)
 * @param accidentPeriods number of accident periods (chain-ladder)
 * @param mfadPct margin for adverse deviation %
 * @param ulaePct ULAE provision %
 * @param expectedLossRatio expected loss ratio % (liability adequacy test)
 * @param treatyCommissionPct reinsurance commission % on treaty premium
 * @param facCommissionPct reinsurance commission % on FAC premium
 * @param remarks remarks
 */
public record ReserveParameterRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 20) String businessLine,
    @NotNull LocalDate effectiveFrom,
    @NotNull IbnrMethod ibnrMethod,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal ibnrRate,
    @NotNull TriangleBasis triangleBasis,
    @NotNull DevelopmentPeriod developmentPeriod,
    @Min(2) @Max(20) int accidentPeriods,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal mfadPct,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal ulaePct,
    @NotNull @DecimalMin("0") @DecimalMax("500") BigDecimal expectedLossRatio,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal treatyCommissionPct,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal facCommissionPct,
    @Size(max = 300) String remarks) {

  /**
   * Domain values.
   *
   * @return terms
   */
  public ReserveParameterTerms toTerms() {
    return new ReserveParameterTerms(
        ibnrMethod,
        ibnrRate,
        triangleBasis,
        developmentPeriod,
        accidentPeriods,
        mfadPct,
        ulaePct,
        expectedLossRatio,
        treatyCommissionPct,
        facCommissionPct,
        remarks);
  }
}
