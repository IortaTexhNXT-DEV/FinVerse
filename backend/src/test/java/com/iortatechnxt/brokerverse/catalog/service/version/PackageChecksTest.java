package com.iortatechnxt.brokerverse.catalog.service.version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.catalog.domain.PackageInsurerRole;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersion;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.SchemeTerms;
import com.iortatechnxt.brokerverse.catalog.domain.ValidationCheck;
import com.iortatechnxt.brokerverse.catalog.domain.ValidationCheck.Result;
import com.iortatechnxt.brokerverse.catalog.domain.VersionCoverage;
import com.iortatechnxt.brokerverse.catalog.domain.VersionInsurer;
import com.iortatechnxt.brokerverse.catalog.domain.VersionInsurerTerm;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The structured checks of the validation checkpoint (PMADD06). */
class PackageChecksTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

  private static VersionInsurer insurer(String code, PackageInsurerRole role, String share) {
    return new VersionInsurer(
        1L, code, role, share == null ? null : new BigDecimal(share), null, null, null);
  }

  private static VersionInsurerTerm term(String insurer, String coverage) {
    return new VersionInsurerTerm(
        insurer, coverage, true, null, null, null, null, null, null, null);
  }

  private static ProductVersion version(
      int no,
      String rate,
      LocalDate from,
      List<VersionInsurer> insurers,
      List<VersionInsurerTerm> terms) {
    return new ProductVersion(
        "MTR30",
        no,
        new ProductVersion.Content(
            from,
            from,
            from.plusYears(1),
            null,
            new SchemeTerms(
                rate == null ? null : new BigDecimal(rate),
                new BigDecimal("3500.00"),
                new BigDecimal("15"),
                null,
                null,
                false),
            List.of(
                new VersionCoverage("OD_THEFT", true, false, null, null, null, null, null, 1),
                new VersionCoverage("AON", true, false, null, null, null, null, null, 2),
                new VersionCoverage("RSCC", false, false, null, null, null, null, null, 3)),
            insurers,
            terms),
        new ProductVersion.Origin("PKR-2026-000001", "MC-1", "Rates"));
  }

  private static RiskProduct product(String coverType) {
    RiskProduct product = mock(RiskProduct.class);
    when(product.getCoverTypeCode()).thenReturn(coverType);
    return product;
  }

  @Test
  void aCompleteVersionPassesWithTheFiguresCompared() {
    ProductVersion previous = version(1, "1.20", TODAY.minusDays(30), List.of(), List.of());
    ProductVersion v =
        version(
            2,
            "1.6",
            TODAY.plusDays(1),
            List.of(
                insurer("INS-A", PackageInsurerRole.PANEL, null),
                insurer("INS-B", PackageInsurerRole.PANEL, null)),
            List.of(
                term("INS-A", "OD_THEFT"),
                term("INS-A", "AON"),
                term("INS-B", "OD_THEFT"),
                term("INS-B", "AON")));
    List<ValidationCheck> checks =
        PackageChecks.run(
            product("PC"),
            v,
            new PackageChecks.Context(
                "OD_THEFT"::equals,
                previous,
                TODAY,
                new BigDecimal("16000"),
                new BigDecimal("1000000")));
    assertThat(checks)
        .extracting(ValidationCheck::code)
        .containsExactly("HIERARCHY", "INSURER_TERMS", "SHARES", "RATES", "DATES", "TEST_PREMIUM");
    assertThat(checks)
        .extracting(ValidationCheck::result)
        .containsExactly(
            Result.PASSED,
            Result.PASSED,
            Result.NOT_APPLICABLE,
            Result.PASSED,
            Result.PASSED,
            Result.PASSED);
    assertThat(checks.get(0).detail())
        .isEqualTo("Cover type PC; 2 included coverages, 1 basic (OD_THEFT)");
    assertThat(checks.get(1).detail())
        .isEqualTo("2 of 2 insurers have terms for 2 included coverages");
    assertThat(checks.get(3).detail())
        .isEqualTo("Package rate 1.60%, minimum premium 3,500.00 (version 1: 1.20%, 3,500.00)");
    assertThat(checks.get(4).detail())
        .startsWith("Effective 29-Sep-2026, package end 29-Sep-2027 (365 days); version 1 started");
    assertThat(checks.get(5).detail())
        .isEqualTo("16,000.00 on a sum insured of 1,000,000.00 at 1.60%, minimum premium 3,500.00");
    assertThat(checks).extracting(ValidationCheck::seq).containsExactly(1, 2, 3, 4, 5, 6);
  }

  @Test
  void anIncompleteVersionFailsTheChecksItMisses() {
    ProductVersion v =
        version(
            1,
            null,
            TODAY.minusDays(1),
            List.of(
                insurer("INS-A", PackageInsurerRole.LEAD, "60"),
                insurer("INS-B", PackageInsurerRole.PARTICIPANT, "30")),
            List.of(term("INS-A", "OD_THEFT")));
    List<ValidationCheck> checks =
        PackageChecks.run(
            product(null),
            v,
            new PackageChecks.Context(c -> false, null, TODAY, null, new BigDecimal("1000000")));
    assertThat(checks)
        .extracting(ValidationCheck::result)
        .containsExactly(
            Result.FAILED,
            Result.FAILED,
            Result.FAILED,
            Result.FAILED,
            Result.FAILED,
            Result.NOT_APPLICABLE);
    assertThat(checks.get(0).detail())
        .isEqualTo("Cover type missing; 2 included coverages, no basic coverage");
    assertThat(checks.get(1).detail())
        .isEqualTo(
            "0 of 2 insurers have terms for 2 included coverages;"
                + " missing: INS-A (AON), INS-B (OD_THEFT, AON)");
    assertThat(checks.get(2).detail()).isEqualTo("Shares 60.00% + 30.00% = 90.00%");
    assertThat(checks.get(3).detail())
        .isEqualTo("No package rate; 0 of 2 insurers rated, minimum premium 3,500.00");
    assertThat(checks.get(4).detail()).endsWith("; effective date before today");
    assertThat(checks.get(5).detail())
        .isEqualTo("Not computed: rated per insurer, no package rate");
  }

  @Test
  void aVersionWithoutInsurersHasNoTermsToCheck() {
    ProductVersion v = version(1, "1.2", TODAY, List.of(), List.of());
    List<ValidationCheck> checks =
        PackageChecks.run(
            product("PC"),
            v,
            new PackageChecks.Context(c -> true, null, TODAY, null, BigDecimal.ONE));
    assertThat(checks.get(1).result()).isEqualTo(Result.NOT_APPLICABLE);
    assertThat(checks.get(1).detail()).isEqualTo("No insurer on the panel");
    assertThat(checks.get(5).detail()).isEqualTo("Not computed for this product");
  }

  @Test
  void theDetailsNameTheCoveragesCoverTypeAndInsurers() {
    ProductVersion v =
        version(
            1,
            "1.6",
            TODAY,
            List.of(insurer("INS-A", PackageInsurerRole.PANEL, null)),
            List.of(term("INS-A", "OD_THEFT")));
    PackageChecks.Names names =
        new PackageChecks.Names() {
          @Override
          public String coverage(String code) {
            return "OD_THEFT".equals(code) ? "Own Damage and Theft" : "Acts of Nature";
          }

          @Override
          public String coverType(String code) {
            return "Private Car";
          }

          @Override
          public String insurer(String code) {
            return "Alpha Insurance";
          }
        };
    List<ValidationCheck> checks =
        PackageChecks.run(
            product("PC"),
            v,
            new PackageChecks.Context(
                "OD_THEFT"::equals, null, TODAY, null, BigDecimal.ONE, names));
    assertThat(checks.get(0).detail())
        .isEqualTo("Cover type Private Car; 2 included coverages, 1 basic (Own Damage and Theft)");
    assertThat(checks.get(1).detail())
        .isEqualTo(
            "0 of 1 insurer has terms for 2 included coverages;"
                + " missing: Alpha Insurance (Acts of Nature)");
  }
}
