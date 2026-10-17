package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.PaymentGate;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct.ProductDetails;
import com.iortatechnxt.brokerverse.catalog.domain.TsuInvolvement;
import com.iortatechnxt.brokerverse.catalog.service.ProductDescriptions;
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
 * @param description package description (Product Matrix)
 * @param incentiveEligible Incentive Eligible (default No)
 * @param incentiveAmount incentive amount (greater than zero)
 * @param incentiveRate incentive commission rate (above 0% up to 100%)
 * @param policyType policy type (list PKG_POLICY_TYPE)
 * @param insuredName Insured's Name of a client-specific package
 * @param approver the approver who is told of the record (optional)
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
    TsuInvolvement tsuInvolvement,
    @Size(max = 500) String description,
    Boolean incentiveEligible,
    BigDecimal incentiveAmount,
    BigDecimal incentiveRate,
    @Size(max = 30) String policyType,
    @Size(max = 250) String insuredName,
    @Size(max = 50) String approver) {

  /**
   * The Product Matrix attributes: description, Annex A attributes and the approver to tell.
   *
   * @return attributes
   */
  public ProductDescriptions.Extras extras() {
    return new ProductDescriptions.Extras(
        description,
        new RiskProduct.MatrixAttributes(
            Boolean.TRUE.equals(incentiveEligible),
            incentiveAmount,
            incentiveRate,
            policyType,
            insuredName),
        approver);
  }

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
