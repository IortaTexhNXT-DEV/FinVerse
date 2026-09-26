package com.iortatechnxt.brokerverse.catalog;

import com.iortatechnxt.brokerverse.catalog.domain.PackageInsurerRole;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSetupService;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSpec;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionService;
import com.iortatechnxt.brokerverse.catalog.service.version.VersionRef;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Package products for the Product Maintenance tests: a new MOTOR package set up by MBS with two
 * panel insurers, released by the TSU Head (PMADD06). Each call creates its own product.
 */
@Component
public class PackageFixtures {

  /** Scheme rate of the packages created here. */
  public static final BigDecimal RATE = new BigDecimal("1.40");

  private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 100_000L);
  private static final String MGIC = "INS-MGIC";
  private static final String LAC = "INS-LAC";

  private final PackageSetupService setup;
  private final ProductVersionService versions;
  private final JdbcTemplate jdbc;
  private final AsUser as;
  private final TestData data;
  private final Clock clock;

  PackageFixtures(
      PackageSetupService setup,
      ProductVersionService versions,
      JdbcTemplate jdbc,
      AsUser as,
      TestData data,
      Clock clock) {
    this.setup = setup;
    this.versions = versions;
    this.jdbc = jdbc;
    this.as = as;
    this.data = data;
    this.clock = clock;
  }

  /** A unique risk code. */
  public static String code() {
    return "PV"
        + Long.toString(SEQ.incrementAndGet() * 7919 + System.nanoTime() % 997, 36).toUpperCase();
  }

  /** Today. */
  public LocalDate today() {
    return LocalDate.now(clock);
  }

  /** The company. */
  public Long company() {
    return data.company().getId();
  }

  /**
   * The content of a new MOTOR package.
   *
   * @param code risk code
   * @param newProduct whether the product is created with the version
   * @param effectiveFrom effective date
   * @param rate scheme rate
   * @return spec
   */
  public PackageSpec spec(
      String code, boolean newProduct, LocalDate effectiveFrom, BigDecimal rate) {
    return new PackageSpec(
        company(),
        code,
        newProduct
            ? new PackageSpec.NewProduct(
                "Test package " + code, "MOTOR", "COMPREHENSIVE", List.of("CBG"), null)
            : null,
        null,
        new PackageSpec.RateScheme(rate, new BigDecimal("3000"), new BigDecimal("15"), null, null),
        new PackageSpec.PackageDates(
            effectiveFrom, effectiveFrom, effectiveFrom.plusYears(1), null),
        List.of(coverage("OD_THEFT", 10), coverage("PD", 20)),
        List.of(insurer(MGIC, null), insurer(LAC, new BigDecimal("1.50"))),
        List.of(term(MGIC, "OD_THEFT"), term(MGIC, "PD"), term(LAC, "OD_THEFT"), term(LAC, "PD")),
        new PackageSpec.Origin("PKR-TEST-" + code, "MC-" + code, "Test set-up"));
  }

  /**
   * A new package whose version 1 is released today.
   *
   * @return risk code
   */
  public String released() {
    String code = code();
    VersionRef ref = as.run("mbs", () -> setup.createDraftVersion(spec(code, true, today(), RATE)));
    release(code, ref.versionNo());
    return code;
  }

  /**
   * Submits (mbs) and validates (tsuhead) a draft.
   *
   * @param code risk code
   * @param versionNo version
   */
  public void release(String code, int versionNo) {
    as.run("mbs", () -> versions.submitForValidation(code, versionNo));
    as.run("tsuhead", () -> versions.validate(code, versionNo, List.of("Hierarchy complete")));
  }

  /**
   * Moves the start of a released version to 2020-01-01 (a package sold for years).
   *
   * @param code risk code
   * @param versionNo version
   */
  public void backdate(String code, int versionNo) {
    jdbc.update(
        "update cat_product_version set effective_from = date '2020-01-01',"
            + " package_start_date = date '2020-01-01' where product_code = ? and version_no = ?",
        code,
        versionNo);
  }

  /**
   * A new-business query of one vehicle.
   *
   * @param code risk code
   * @param insurer insurer, may be null
   * @param rate item rate, null for the scheme rate
   * @return query
   */
  public RatingQuery query(String code, String insurer, BigDecimal rate) {
    return new RatingQuery(
        company(),
        code,
        insurer,
        null,
        List.of(new RatingQuery.Item("ABC 1234", new BigDecimal("1000000"), rate, null, null)),
        false,
        null,
        today(),
        today().plusYears(1),
        null,
        false,
        null);
  }

  private static PackageSpec.Coverage coverage(String code, int order) {
    return new PackageSpec.Coverage(code, true, false, null, null, null, order);
  }

  private static PackageSpec.Insurer insurer(String code, BigDecimal rate) {
    return new PackageSpec.Insurer(code, PackageInsurerRole.PANEL, null, rate, null, null);
  }

  private static PackageSpec.InsurerTerm term(String insurer, String coverage) {
    return new PackageSpec.InsurerTerm(
        insurer,
        coverage,
        true,
        null,
        null,
        new PackageSpec.Deductible(new BigDecimal("2000"), null, null),
        List.of("GEN_SANCTIONS"),
        null);
  }
}
