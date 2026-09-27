package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.PaymentGate;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct.ProductDetails;
import com.iortatechnxt.brokerverse.catalog.domain.TsuInvolvement;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * New or changed risk product (BRNB.001).
 *
 * @param code risk code (ignored on update)
 * @param name name
 * @param lineCode product line
 * @param coverTypeCode cover type
 * @param packaged package product
 * @param fleetCapable fleet capable
 * @param marketSegments segments allowed (empty = all)
 * @param mortgageApplicable mortgage applicable
 * @param directPaymentEligible direct payment eligible
 * @param multiYearAllowed multi-year allowed
 * @param maxTermYears longest term in years
 * @param ffyEligible Free First Year eligible
 * @param paymentGate payment gate
 * @param defaultRate default premium rate %
 * @param defaultCommissionRate default commission %
 * @param minimumPremium minimum premium
 * @param maxSumInsured package TSI limit
 * @param tsuInvolvement TSU involvement
 */
public record ProductRequest(
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z0-9]+", message = "use A-Z and 0-9")
        String code,
    @NotBlank @Size(max = 200) String name,
    @NotBlank String lineCode,
    String coverTypeCode,
    boolean packaged,
    boolean fleetCapable,
    List<String> marketSegments,
    boolean mortgageApplicable,
    boolean directPaymentEligible,
    boolean multiYearAllowed,
    @Min(1) @Max(10) int maxTermYears,
    boolean ffyEligible,
    @NotNull PaymentGate paymentGate,
    @DecimalMin("0") @DecimalMax("100") BigDecimal defaultRate,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal defaultCommissionRate,
    @NotNull @DecimalMin("0") BigDecimal minimumPremium,
    @DecimalMin("0.01") BigDecimal maxSumInsured,
    TsuInvolvement tsuInvolvement) {

  /**
   * Maintainable attributes.
   *
   * @return details
   */
  public ProductDetails details() {
    return new ProductDetails(
        name.trim(),
        lineCode,
        coverTypeCode == null || coverTypeCode.isBlank() ? null : coverTypeCode,
        packaged,
        fleetCapable,
        marketSegments == null ? List.of() : List.copyOf(marketSegments),
        mortgageApplicable,
        directPaymentEligible,
        multiYearAllowed,
        maxTermYears,
        ffyEligible,
        paymentGate,
        defaultRate,
        defaultCommissionRate,
        minimumPremium,
        maxSumInsured,
        tsuInvolvement);
  }
}
