package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Premium calculator input (Appendix A).
 *
 * @param companyId company
 * @param productCode risk code
 * @param insurerCode insurer party code (commission), optional
 * @param branchCode insurer branch (LGT), optional
 * @param items items to rate
 * @param multiYear multi-year cover
 * @param basis period basis (annual when empty)
 * @param periodFrom period start
 * @param periodTo period end
 * @param commissionRate commission override %, optional
 * @param endorsement endorsement (no minimum premium)
 * @param schemeVersion package version to price on (history look-up of the calculator, BRPM.007),
 *     empty for the version in force
 */
public record RatingRequest(
    @NotNull Long companyId,
    @NotBlank String productCode,
    String insurerCode,
    String branchCode,
    @NotEmpty @Size(max = 500) List<@Valid Item> items,
    boolean multiYear,
    PeriodBasis basis,
    LocalDate periodFrom,
    LocalDate periodTo,
    @DecimalMin("0") @DecimalMax("100") BigDecimal commissionRate,
    boolean endorsement,
    @Positive Integer schemeVersion) {

  /**
   * One item to rate.
   *
   * @param label label
   * @param sumInsured sum insured (negative only for an endorsement that reduces the cover)
   * @param ratePercent rate % (empty for the product default)
   * @param biLimit motor excess BI limit
   * @param pdLimit motor PD limit
   */
  public record Item(
      @Size(max = 200) String label,
      @NotNull BigDecimal sumInsured,
      @DecimalMin("0") @DecimalMax("100") BigDecimal ratePercent,
      BigDecimal biLimit,
      BigDecimal pdLimit) {}

  /**
   * The service query.
   *
   * @return query
   */
  public RatingQuery query() {
    return new RatingQuery(
        companyId,
        productCode,
        blankToNull(insurerCode),
        blankToNull(branchCode),
        items.stream()
            .map(
                i ->
                    new RatingQuery.Item(
                        i.label(), i.sumInsured(), i.ratePercent(), i.biLimit(), i.pdLimit()))
            .toList(),
        multiYear,
        basis,
        periodFrom,
        periodTo,
        commissionRate,
        endorsement,
        null,
        schemeVersion == null ? null : RatingQuery.Purpose.RENEWAL,
        schemeVersion,
        null);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
