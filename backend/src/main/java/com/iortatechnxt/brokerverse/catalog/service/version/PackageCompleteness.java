package com.iortatechnxt.brokerverse.catalog.service.version;

import com.iortatechnxt.brokerverse.catalog.domain.PackageInsurerRole;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersion;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.SchemeTerms;
import com.iortatechnxt.brokerverse.catalog.domain.VersionCoverage;
import com.iortatechnxt.brokerverse.catalog.domain.VersionInsurer;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Completeness of a package version before it is submitted and again when it is validated
 * (PMADD01/02/06; PRODUCT_MAINTENANCE_DESIGN section 5.1). Pure checks; each failure is a
 * BusinessRuleException with the code named in the design.
 */
public final class PackageCompleteness {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final String DATES_INVALID = "PACKAGE_DATES_INVALID";

  private PackageCompleteness() {}

  /**
   * Checks a version.
   *
   * @param product the package
   * @param version the version
   * @param basicCoverage whether a coverage code of the product line is a basic coverage
   * @param latest latest released version (current or future), null when none
   * @param today business date
   */
  public static void check(
      RiskProduct product,
      ProductVersion version,
      Predicate<String> basicCoverage,
      ProductVersion latest,
      LocalDate today) {
    hierarchy(product, version, basicCoverage);
    insurerTerms(version);
    rate(version);
    dates(version, latest, today);
  }

  private static void hierarchy(
      RiskProduct product, ProductVersion version, Predicate<String> basicCoverage) {
    boolean basic =
        version.getCoverages().stream()
            .filter(VersionCoverage::included)
            .map(VersionCoverage::coverageCode)
            .anyMatch(basicCoverage);
    if (product.getCoverTypeCode() == null || !basic) {
      throw new BusinessRuleException(
          "PACKAGE_HIERARCHY_INCOMPLETE",
          "The package needs its cover type and at least one included basic coverage (PMADD01)");
    }
  }

  private static void insurerTerms(ProductVersion version) {
    List<String> included =
        version.getCoverages().stream()
            .filter(VersionCoverage::included)
            .map(VersionCoverage::coverageCode)
            .toList();
    for (VersionInsurer insurer : version.getInsurers()) {
      for (String coverage : included) {
        boolean present =
            version.getInsurerTerms().stream()
                .anyMatch(
                    t ->
                        t.insurerCode().equals(insurer.insurerCode())
                            && t.coverageCode().equals(coverage));
        if (!present) {
          throw new BusinessRuleException(
              "PACKAGE_INSURER_TERMS_MISSING",
              "Insurer " + insurer.insurerCode() + " has no terms for coverage " + coverage);
        }
      }
    }
    List<BigDecimal> shares =
        version.getInsurers().stream()
            .filter(i -> i.role() != PackageInsurerRole.PANEL)
            .map(VersionInsurer::sharePercent)
            .filter(Objects::nonNull)
            .toList();
    if (!shares.isEmpty()
        && shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(HUNDRED) != 0) {
      throw new BusinessRuleException(
          "PACKAGE_SHARES_INVALID", "The co-insurance shares must add up to 100%");
    }
  }

  private static void rate(ProductVersion version) {
    SchemeTerms scheme = version.getScheme();
    boolean everyInsurerRated =
        !version.getInsurers().isEmpty()
            && version.getInsurers().stream().allMatch(i -> i.rate() != null);
    if (scheme.defaultRate() == null && !everyInsurerRated) {
      throw new BusinessRuleException(
          "PACKAGE_RATE_MISSING", "Enter the package rate or a rate for every insurer");
    }
  }

  private static void dates(ProductVersion version, ProductVersion current, LocalDate today) {
    LocalDate from = version.getEffectiveFrom();
    if (from.isBefore(today)) {
      throw new BusinessRuleException(DATES_INVALID, "The effective date cannot be before today");
    }
    if (version.getPackageEndDate() != null && !version.getPackageEndDate().isAfter(from)) {
      throw new BusinessRuleException(
          DATES_INVALID, "The package end date must be after the effective date");
    }
    if (current != null && !from.isAfter(current.getEffectiveFrom())) {
      throw new BusinessRuleException(
          DATES_INVALID,
          "The effective date must be after the start of version " + current.getVersionNo());
    }
  }
}
