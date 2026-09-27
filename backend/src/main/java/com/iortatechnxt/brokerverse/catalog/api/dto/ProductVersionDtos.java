package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.ProductVersion;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionStatus;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.SchemeTerms;
import com.iortatechnxt.brokerverse.catalog.domain.VersionCoverage;
import com.iortatechnxt.brokerverse.catalog.domain.VersionInsurer;
import com.iortatechnxt.brokerverse.catalog.domain.VersionInsurerTerm;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Request and response bodies of package versions (BRPM.006/007, PMADD01/02/06;
 * PRODUCT_MAINTENANCE_DESIGN section 10).
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class ProductVersionDtos {

  private ProductVersionDtos() {}

  /**
   * One version in a list (Versions tab, Validation Queue).
   *
   * @param productCode risk code
   * @param productName product name
   * @param versionNo version
   * @param status status
   * @param effectiveFrom sells from
   * @param effectiveTo sells until, null while current
   * @param packageEndDate package end date
   * @param defaultRate scheme rate %
   * @param minimumPremium minimum premium
   * @param sourceRequestNo package request, null for a catalog-only version
   * @param changeSummary what changed
   * @param maker who set it up
   * @param submittedBy who submitted it
   * @param submittedAt when
   * @param validatedBy validator
   * @param validatedAt when
   */
  public record VersionSummary(
      String productCode,
      String productName,
      int versionNo,
      ProductVersionStatus status,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      LocalDate packageEndDate,
      BigDecimal defaultRate,
      BigDecimal minimumPremium,
      String sourceRequestNo,
      String changeSummary,
      String maker,
      String submittedBy,
      Instant submittedAt,
      String validatedBy,
      Instant validatedAt) {

    /**
     * Maps a version.
     *
     * @param v version
     * @param productName product name
     * @return summary
     */
    public static VersionSummary from(ProductVersion v, String productName) {
      return new VersionSummary(
          v.getProductCode(),
          productName,
          v.getVersionNo(),
          v.getStatus(),
          v.getEffectiveFrom(),
          v.getEffectiveTo(),
          v.getPackageEndDate(),
          v.getScheme().defaultRate(),
          v.getScheme().minimumPremium(),
          v.getSourceRequestNo(),
          v.getChangeSummary(),
          v.getCreatedBy(),
          v.getSubmittedBy(),
          v.getSubmittedAt(),
          v.getValidatedBy(),
          v.getValidatedAt());
    }
  }

  /**
   * A version with its content and checkpoint (Version editor).
   *
   * @param summary list facts
   * @param lineCode product line
   * @param coverTypeCode cover type
   * @param packageStartDate start of the insurer agreement
   * @param anniversaryDate anniversary date
   * @param scheme rate scheme
   * @param coverages coverages
   * @param insurers insurers
   * @param insurerTerms insurer x coverage terms
   * @param mancomSignoffRef ManCom sign-off reference
   * @param validationChecklist confirmed checklist (JSON array)
   * @param testPremium test premium computed at validation
   * @param returnedReason reason of the last return
   */
  public record VersionDetail(
      VersionSummary summary,
      String lineCode,
      String coverTypeCode,
      LocalDate packageStartDate,
      LocalDate anniversaryDate,
      SchemeTerms scheme,
      List<VersionCoverage> coverages,
      List<VersionInsurer> insurers,
      List<VersionInsurerTerm> insurerTerms,
      String mancomSignoffRef,
      String validationChecklist,
      BigDecimal testPremium,
      String returnedReason) {

    /**
     * Maps a version.
     *
     * @param v version
     * @param product its product
     * @return detail
     */
    public static VersionDetail from(ProductVersion v, RiskProduct product) {
      return new VersionDetail(
          VersionSummary.from(v, product.getName()),
          product.getLineCode(),
          product.getCoverTypeCode(),
          v.getPackageStartDate(),
          v.getAnniversaryDate(),
          v.getScheme(),
          v.getCoverages(),
          v.getInsurers(),
          v.getInsurerTerms(),
          v.getMancomSignoffRef(),
          v.getValidationChecklist(),
          v.getTestPremium(),
          v.getReturnedReason());
    }
  }

  /**
   * "New Version": a draft copied from the version in force.
   *
   * @param companyId company (insurer look-ups)
   * @param changeSummary what is going to change
   */
  public record NewVersionRequest(
      @NotNull Long companyId, @NotBlank @Size(max = 1000) String changeSummary) {}

  /**
   * The content of a DRAFT version.
   *
   * @param companyId company (insurer look-ups)
   * @param rateScheme rate scheme
   * @param dates effectivity and package term
   * @param coverages coverages
   * @param insurers insurers
   * @param insurerTerms insurer x coverage terms
   * @param changeSummary what changes against the previous version
   * @param mancomSignoffRef ManCom sign-off reference, may be empty
   */
  public record VersionContentRequest(
      @NotNull Long companyId,
      @NotNull PackageSpec.RateScheme rateScheme,
      @NotNull PackageSpec.PackageDates dates,
      @Size(max = 100) List<PackageSpec.@Valid Coverage> coverages,
      @Size(max = 50) List<PackageSpec.@Valid Insurer> insurers,
      @Size(max = 2000) List<PackageSpec.@Valid InsurerTerm> insurerTerms,
      @Size(max = 1000) String changeSummary,
      @Size(max = 60) String mancomSignoffRef) {

    /**
     * The set-up spec of an existing product.
     *
     * @param productCode risk code
     * @param sourceRequestNo package request the draft came from, kept
     * @return spec
     */
    public PackageSpec spec(String productCode, String sourceRequestNo) {
      return new PackageSpec(
          companyId,
          productCode,
          null,
          null,
          rateScheme,
          dates,
          coverages,
          insurers,
          insurerTerms,
          new PackageSpec.Origin(sourceRequestNo, mancomSignoffRef, changeSummary));
    }
  }

  /**
   * Validation of a version (PMADD06).
   *
   * @param checklist checklist items the validator confirmed
   */
  public record ValidateRequest(
      @Size(max = 20) List<@NotBlank @Size(max = 200) String> checklist) {}
}
