package com.iortatechnxt.brokerverse.catalog.service.version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.catalog.domain.PackageInsurerRole;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionStatus;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The temporary in-memory stubs of the package version contracts (P1 wave). */
class PackageVersionStubDefaultsTest {

  private final PackageVersionStubDefaults defaults = new PackageVersionStubDefaults();
  private final PackageSetupService setup = defaults.stubPackageSetupService();
  private final ProductVersionQueryService query = defaults.stubProductVersionQueryService();

  private static PackageSpec spec(String code, String summary) {
    PackageSpec.Deductible deductible =
        new PackageSpec.Deductible(new BigDecimal("1000.00"), null, null);
    return new PackageSpec(
        1L,
        code,
        new PackageSpec.NewProduct("Motor Package Test", "MTR", "PC", List.of("RETAIL"), null),
        null,
        new PackageSpec.RateScheme(
            new BigDecimal("2.50"), new BigDecimal("5000.00"), new BigDecimal("15"), null, null),
        new PackageSpec.PackageDates(
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30), null),
        List.of(new PackageSpec.Coverage("OD", true, false, null, null, deductible, 10)),
        List.of(
            new PackageSpec.Insurer(
                "INS-A", PackageInsurerRole.PANEL, null, new BigDecimal("2.40"), null, null)),
        List.of(
            new PackageSpec.InsurerTerm(
                "INS-A", "OD", true, null, null, deductible, List.of("WAR-01"), null)),
        new PackageSpec.Origin("PKR-2026-1", "MC-1", summary));
  }

  @Test
  void draftsAreNumberedKeptAndReplacedInMemory() {
    VersionRef v1 = setup.createDraftVersion(spec("STUB01", "first"));
    assertThat(v1)
        .isEqualTo(
            new VersionRef("STUB01", 1, ProductVersionStatus.DRAFT, LocalDate.of(2026, 10, 1)));
    assertThatThrownBy(() -> setup.createDraftVersion(spec("STUB01", "second")))
        .extracting("code")
        .isEqualTo("VERSION_IN_PROGRESS");

    VersionRef updated = setup.updateDraftVersion("STUB01", 1, spec("STUB01", "changed"));
    assertThat(updated.versionNo()).isEqualTo(1);
    ProductVersionView view = query.version("STUB01", 1).orElseThrow();
    assertThat(view.origin().changeSummary()).isEqualTo("changed");
    assertThat(view.productName()).isEqualTo("Motor Package Test");
    assertThat(view.ref()).isEqualTo(updated);
    assertThat(view.insurerTerms().get(0).clauseCodes()).containsExactly("WAR-01");
    assertThat(query.versions("STUB01")).hasSize(1);
    assertThat(query.version("STUB01", 2)).isEmpty();

    assertThatThrownBy(() -> setup.updateDraftVersion("STUB01", 3, spec("STUB01", "x")))
        .hasMessageContaining("STUB01");
  }

  @Test
  void nothingIsEverReleasedOrExpiring() {
    setup.createDraftVersion(spec("STUB02", "draft"));
    setup.retireProduct("STUB02", new PackageSpec.Origin("PKR-2026-2", null, "retire"));
    assertThat(query.current("STUB02")).isEmpty();
    assertThat(query.inForce("STUB02", LocalDate.of(2026, 12, 1))).isEmpty();
    assertThat(query.packagesExpiring(1L, 90)).isEmpty();
    assertThat(query.versions("UNKNOWN")).isEmpty();
    assertThatThrownBy(() -> setup.createDraftVersion(spec(" ", "no code")))
        .extracting("code")
        .isEqualTo("PRODUCT_CODE_REQUIRED");
  }

  @Test
  void contractRecordsCopyTheirLists() {
    PackageSpec empty = new PackageSpec(1L, "X", null, 1, null, null, null, null, null, null);
    assertThat(empty.coverages()).isEmpty();
    assertThat(empty.insurers()).isEmpty();
    assertThat(empty.insurerTerms()).isEmpty();
    assertThat(new PackageSpec.NewProduct("n", "L", "C", null, null).marketSegments()).isEmpty();
    assertThat(
            new PackageSpec.InsurerTerm("I", "C", true, null, null, null, null, null).clauseCodes())
        .isEmpty();
    ProductVersionView view =
        new ProductVersionView(
            "X",
            "X",
            1,
            ProductVersionStatus.RELEASED,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    assertThat(view.ref().effectiveFrom()).isNull();
    assertThat(view.coverages()).isEmpty();
    assertThat(RatingQuery.Purpose.valueOf("RENEWAL")).isEqualTo(RatingQuery.Purpose.RENEWAL);
    assertThat(
            new IncentiveCriteriaChanged(
                    1L,
                    "CPC2",
                    IncentiveCriteriaChanged.Change.AMENDED,
                    LocalDate.of(2026, 1, 1),
                    null)
                .change())
        .isEqualTo(IncentiveCriteriaChanged.Change.AMENDED);
  }
}
