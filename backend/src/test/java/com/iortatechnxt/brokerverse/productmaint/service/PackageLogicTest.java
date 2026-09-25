package com.iortatechnxt.brokerverse.productmaint.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.catalog.domain.PackageInsurerRole;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionStatus;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSpec;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionView;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageInsurerResponse;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageInsurerResponse.ResponseInput;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.CoverageTerm;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.Dates;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.InsurerLine;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.Scheme;
import com.iortatechnxt.brokerverse.productmaint.service.ComparativeTable.Selection;
import com.iortatechnxt.brokerverse.productmaint.service.VersionTerms.SetupFacts;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Pure logic of the package process: comparative compilation, expiry buckets and dates, specs. */
class PackageLogicTest {

  private static final LocalDate START = LocalDate.of(2027, 1, 1);

  private static final CoverageTerm OWN_DAMAGE =
      new CoverageTerm(
          "OWN_DAMAGE",
          true,
          false,
          new BigDecimal("1000000"),
          null,
          new BigDecimal("3000"),
          new BigDecimal("0.5"),
          "each claim",
          List.of("CL01"),
          null);

  private static PackageInsurerResponse response(String code, String outcome, String rate) {
    PackageInsurerResponse r = new PackageInsurerResponse(1L, code, code + " Insurance");
    if (outcome != null) {
      r.record(
          new ResponseInput(
              outcome,
              rate == null ? null : new BigDecimal(rate),
              null,
              "[]",
              "cond",
              START,
              "rem"),
          Instant.EPOCH);
    }
    return r;
  }

  @Test
  void theComparativePutsOffersFirstCheapestFirstAndSelectsFieldsAndInsurers() {
    ComparativeTable t =
        ComparativeTable.compile(
            2,
            List.of(
                response("A", "APPROVED_WITH_CHANGES", "0.40"),
                response("B", "DECLINED", null),
                response("C", "COUNTER_PROPOSAL", "0.30"),
                response("D", null, null)),
            json -> List.of(OWN_DAMAGE));
    assertThat(t.rows())
        .extracting(ComparativeTable.Row::insurerCode)
        .containsExactly("C", "A", "B", "D");
    assertThat(t.rows().get(0).lowest()).isTrue();
    assertThat(t.rows().get(0).coverages()).isEqualTo("OWN_DAMAGE (1,000,000.00)");
    assertThat(t.rows().get(0).deductibles()).isEqualTo("OWN_DAMAGE: each claim / 3,000.00 / 0.5%");
    assertThat(t.headers()).hasSize(ComparativeTable.FIELDS.size() + 1);

    ComparativeTable client = t.select(new Selection(List.of("REMARKS", "RATE"), List.of("A")));
    assertThat(client.fields()).containsExactly("RATE", "REMARKS");
    assertThat(client.headers()).containsExactly("Insurer", "Rate %", "Remarks");
    assertThat(client.cells()).containsExactly(List.of("A Insurance", "0.40", "rem"));
    assertThat(t.select(Selection.ALL).rows()).hasSize(4);
    assertThat(t.rows().get(1).value("VALID_UNTIL")).isEqualTo("2027-01-01");
    assertThat(t.rows().get(1).value("MINIMUM_PREMIUM")).isEmpty();
    assertThat(t.rows().get(1).value("CONDITIONS")).isEqualTo("cond");
    assertThat(t.rows().get(1).value("OUTCOME")).isEqualTo("APPROVED_WITH_CHANGES");
    assertThatThrownBy(() -> t.select(new Selection(List.of("PREMIUM"), List.of())))
        .extracting("code")
        .isEqualTo("COMPARATIVE_FIELD_UNKNOWN");
    assertThat(ComparativeTable.offered("NO_RESPONSE")).isFalse();
    assertThat(ComparativeTable.offered(null)).isFalse();
  }

  @Test
  void theExpiryBucketAndTheNextTermFollowTheParameters() {
    assertThat(PackageExpiryService.bucket(55, 60, List.of(30, 7))).isEqualTo(60);
    assertThat(PackageExpiryService.bucket(30, 60, List.of(30, 7))).isEqualTo(30);
    assertThat(PackageExpiryService.bucket(3, 60, List.of(7, 30))).isEqualTo(7);
    Dates next =
        PackageExpiryService.rolled(
            new PackageSpec.PackageDates(START, START, START.plusMonths(6).minusDays(1), START));
    assertThat(next.effectiveFrom()).isEqualTo(START.plusMonths(6));
    assertThat(next.anniversaryDate()).isEqualTo(START.plusYears(1));
    Dates unknownStart =
        PackageExpiryService.rolled(new PackageSpec.PackageDates(null, null, START, null));
    assertThat(unknownStart.packageEndDate()).isEqualTo(START.plusYears(1));
    assertThat(unknownStart.anniversaryDate()).isNull();
  }

  @Test
  void proposedTermsBecomeAPackageSpecAndAVersionPrefillsTerms() {
    PackageTerms terms =
        new PackageTerms(
            List.of(),
            List.of(OWN_DAMAGE),
            new Scheme(BigDecimal.ONE, BigDecimal.TEN, null, null, "basis"),
            new Dates(START, START, START.plusYears(1), null),
            List.of(
                new InsurerLine("INS-A", "lead", new BigDecimal("60"), null, null, List.of()),
                new InsurerLine(
                    "INS-B",
                    "PARTICIPANT",
                    new BigDecimal("40"),
                    null,
                    null,
                    List.of(OWN_DAMAGE))));
    PackageSpec spec =
        VersionTerms.spec(
            new SetupFacts(1L, "PMT1", null, 2, new PackageSpec.Origin("PKR-1", "MC-1", "x")),
            terms);
    assertThat(spec.insurers())
        .extracting(PackageSpec.Insurer::role)
        .containsExactly(PackageInsurerRole.LEAD, PackageInsurerRole.PARTICIPANT);
    assertThat(spec.insurerTerms()).hasSize(2);
    assertThat(spec.coverages().get(0).deductible().text()).isEqualTo("each claim");
    assertThat(spec.coverages().get(0).sortOrder()).isEqualTo(10);

    ProductVersionView v =
        new ProductVersionView(
            "PMT1",
            "Test",
            2,
            ProductVersionStatus.DRAFT,
            spec.dates(),
            null,
            spec.rateScheme(),
            spec.coverages(),
            spec.insurers(),
            spec.insurerTerms(),
            spec.origin(),
            null);
    PackageTerms back = VersionTerms.of(v);
    assertThat(back.insurerCodes()).containsExactly("INS-A", "INS-B");
    assertThat(back.insurers().get(0).role()).isEqualTo("LEAD");
    assertThat(back.scheme().ratingBasisNote()).isEqualTo("basis");
    assertThat(back.coverages().get(0).deductibleAmount()).isEqualByComparingTo("3000");
    PackageTerms empty =
        VersionTerms.of(
            new ProductVersionView(
                "X",
                "X",
                1,
                ProductVersionStatus.DRAFT,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));
    assertThat(empty.scheme()).isEqualTo(Scheme.NONE);
    assertThat(Map.of("k", empty.dates())).containsValue(Dates.NONE);
  }
}
