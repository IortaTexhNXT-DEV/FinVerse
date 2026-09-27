package com.iortatechnxt.brokerverse.underwriting.api.dto;

import com.iortatechnxt.brokerverse.underwriting.domain.ProductTerms;
import com.iortatechnxt.brokerverse.underwriting.domain.TaxRates;
import com.iortatechnxt.brokerverse.underwriting.domain.UprBasis;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Create / update product request. {@code companyId} and {@code code} are immutable.
 *
 * @param companyId company
 * @param code product code
 * @param name name
 * @param businessLine line of business (BUSINESS_LINE dimension)
 * @param defaultCommissionRate default commission %
 * @param uprBasis unearned premium basis
 * @param dstRate documentary stamp tax %
 * @param vatRate VAT %
 * @param lgtRate local government tax %
 * @param fstRate fire service tax %
 * @param premiumTaxRate premium tax %
 * @param policyFee flat policy fee
 * @param openCoverAllowed marine open covers allowed
 */
public record ProductRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z0-9\\-]+") String code,
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 20) String businessLine,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal defaultCommissionRate,
    @NotNull UprBasis uprBasis,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal dstRate,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal vatRate,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal lgtRate,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal fstRate,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal premiumTaxRate,
    @NotNull @DecimalMin("0") BigDecimal policyFee,
    boolean openCoverAllowed) {

  /**
   * Domain terms.
   *
   * @return terms
   */
  public ProductTerms toTerms() {
    return new ProductTerms(
        name,
        businessLine,
        defaultCommissionRate,
        uprBasis,
        new TaxRates(dstRate, vatRate, lgtRate, fstRate, premiumTaxRate),
        policyFee,
        openCoverAllowed);
  }
}
