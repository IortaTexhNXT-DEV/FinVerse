package com.iortatechnxt.brokerverse.catalog.service.version;

import com.iortatechnxt.brokerverse.catalog.domain.PackageInsurerRole;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersion;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.SchemeTerms;
import com.iortatechnxt.brokerverse.catalog.domain.ValidationCheck;
import com.iortatechnxt.brokerverse.catalog.domain.ValidationCheck.Result;
import com.iortatechnxt.brokerverse.catalog.domain.VersionCoverage;
import com.iortatechnxt.brokerverse.catalog.domain.VersionInsurer;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The checks of the validation checkpoint of a package version (PMADD06), as a structured list: the
 * same rules as {@link PackageCompleteness}, each with its result and the figures compared, so the
 * validator and MBS read what was checked ("3 of 3 insurers have terms for 2 included coverages")
 * instead of a ticked text. Pure; the version is not changed.
 */
public final class PackageChecks {

  /** Label of the hierarchy check (also the validator's checklist item). */
  public static final String HIERARCHY = "Hierarchy complete: cover type and basic coverage";

  /** Label of the insurer terms check. */
  public static final String INSURER_TERMS =
      "Every panel insurer has terms for each included coverage";

  /** Label of the co-insurance shares check. */
  public static final String SHARES = "Co-insurance shares add up to 100%";

  /** Label of the rates check. */
  public static final String RATES = "Rates and minimum premiums match the signed-off terms";

  /** Label of the dates check. */
  public static final String DATES = "Dates and package term are correct";

  /** Label of the test premium check. */
  public static final String TEST_PREMIUM = "Test premium reviewed";

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private PackageChecks() {}

  /**
   * What the checks compare besides the version itself.
   *
   * @param basicCoverage whether a coverage code of the product line is a basic coverage
   * @param latest latest released version (current or future), null when none
   * @param today business date
   * @param testPremium test premium on the sample item, null when not computable
   * @param sampleSumInsured sum insured of the sample item
   */
  public record Context(
      Predicate<String> basicCoverage,
      ProductVersion latest,
      LocalDate today,
      BigDecimal testPremium,
      BigDecimal sampleSumInsured) {}

  /**
   * Runs the checks.
   *
   * @param product the package
   * @param version the version
   * @param context what the checks compare
   * @return the checks in checklist order
   */
  public static List<ValidationCheck> run(
      RiskProduct product, ProductVersion version, Context context) {
    List<ValidationCheck> checks = new ArrayList<>();
    checks.add(hierarchy(checks.size() + 1, product, version, context.basicCoverage()));
    checks.add(insurerTerms(checks.size() + 1, version));
    checks.add(shares(checks.size() + 1, version));
    checks.add(rates(checks.size() + 1, version, context.latest()));
    checks.add(dates(checks.size() + 1, version, context.latest(), context.today()));
    checks.add(testPremium(checks.size() + 1, version, context));
    return List.copyOf(checks);
  }

  private static List<String> included(ProductVersion version) {
    return version.getCoverages().stream()
        .filter(VersionCoverage::included)
        .map(VersionCoverage::coverageCode)
        .toList();
  }

  private static ValidationCheck hierarchy(
      int seq, RiskProduct product, ProductVersion version, Predicate<String> basicCoverage) {
    List<String> included = included(version);
    List<String> basic = included.stream().filter(basicCoverage).toList();
    String coverType = product.getCoverTypeCode();
    boolean ok = coverType != null && !basic.isEmpty();
    String detail =
        "Cover type "
            + (coverType == null ? "missing" : coverType)
            + "; "
            + count(included.size(), "included coverage")
            + ", "
            + (basic.isEmpty()
                ? "no basic coverage"
                : basic.size() + " basic (" + join(basic) + ")");
    return check(seq, "HIERARCHY", HIERARCHY, ok, detail);
  }

  private static ValidationCheck insurerTerms(int seq, ProductVersion version) {
    List<String> included = included(version);
    List<VersionInsurer> insurers = version.getInsurers();
    if (insurers.isEmpty()) {
      return new ValidationCheck(
          seq, "INSURER_TERMS", INSURER_TERMS, Result.NOT_APPLICABLE, "No insurer on the panel");
    }
    List<String> missing = new ArrayList<>();
    int complete = 0;
    for (VersionInsurer insurer : insurers) {
      List<String> without =
          included.stream()
              .filter(
                  c ->
                      version.getInsurerTerms().stream()
                          .noneMatch(
                              t ->
                                  t.insurerCode().equals(insurer.insurerCode())
                                      && t.coverageCode().equals(c)))
              .toList();
      if (without.isEmpty()) {
        complete++;
      } else {
        missing.add(insurer.insurerCode() + " (" + join(without) + ")");
      }
    }
    String detail =
        complete
            + " of "
            + count(insurers.size(), "insurer")
            + (insurers.size() == 1 ? " has" : " have")
            + " terms for "
            + count(included.size(), "included coverage")
            + (missing.isEmpty() ? "" : "; missing: " + String.join(", ", missing));
    return check(seq, "INSURER_TERMS", INSURER_TERMS, missing.isEmpty(), detail);
  }

  private static ValidationCheck shares(int seq, ProductVersion version) {
    List<BigDecimal> shares =
        version.getInsurers().stream()
            .filter(i -> i.role() != PackageInsurerRole.PANEL)
            .map(VersionInsurer::sharePercent)
            .filter(Objects::nonNull)
            .toList();
    if (shares.isEmpty()) {
      return new ValidationCheck(
          seq, "SHARES", SHARES, Result.NOT_APPLICABLE, "Panel insurers only; no co-insurance");
    }
    BigDecimal total = shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    String detail =
        "Shares "
            + shares.stream().map(s -> percent(s)).collect(Collectors.joining(" + "))
            + " = "
            + percent(total);
    return check(seq, "SHARES", SHARES, total.compareTo(HUNDRED) == 0, detail);
  }

  private static ValidationCheck rates(int seq, ProductVersion version, ProductVersion latest) {
    SchemeTerms scheme = version.getScheme();
    List<VersionInsurer> insurers = version.getInsurers();
    long rated = insurers.stream().filter(i -> i.rate() != null).count();
    boolean everyInsurerRated = !insurers.isEmpty() && rated == insurers.size();
    StringBuilder detail = new StringBuilder();
    if (scheme.defaultRate() != null) {
      detail.append("Package rate ").append(percent(scheme.defaultRate()));
    } else {
      detail.append("No package rate; ").append(rated).append(" of ");
      detail.append(count(insurers.size(), "insurer")).append(" rated");
    }
    detail.append(", minimum premium ").append(DisplayFormat.amount(scheme.minimumPremium()));
    if (latest != null) {
      SchemeTerms previous = latest.getScheme();
      detail
          .append(" (version ")
          .append(latest.getVersionNo())
          .append(": ")
          .append(
              previous.defaultRate() == null ? "insurer rates" : percent(previous.defaultRate()))
          .append(", ")
          .append(DisplayFormat.amount(previous.minimumPremium()))
          .append(')');
    }
    return check(
        seq, "RATES", RATES, scheme.defaultRate() != null || everyInsurerRated, detail.toString());
  }

  private static ValidationCheck dates(
      int seq, ProductVersion version, ProductVersion latest, LocalDate today) {
    LocalDate from = version.getEffectiveFrom();
    LocalDate end = version.getPackageEndDate();
    List<String> problems = new ArrayList<>();
    if (from.isBefore(today)) {
      problems.add("effective date before today");
    }
    if (end != null && !end.isAfter(from)) {
      problems.add("package end not after the effective date");
    }
    if (latest != null && !from.isAfter(latest.getEffectiveFrom())) {
      problems.add("not after the start of version " + latest.getVersionNo());
    }
    StringBuilder detail =
        new StringBuilder("Effective ").append(DisplayFormat.date(from)).append(", package end ");
    if (end == null) {
      detail.append("open");
    } else {
      detail
          .append(DisplayFormat.date(end))
          .append(" (")
          .append(count(ChronoUnit.DAYS.between(from, end), "day"))
          .append(')');
    }
    if (latest != null) {
      detail
          .append("; version ")
          .append(latest.getVersionNo())
          .append(" started ")
          .append(DisplayFormat.date(latest.getEffectiveFrom()));
    }
    if (!problems.isEmpty()) {
      detail.append("; ").append(String.join("; ", problems));
    }
    return check(seq, "DATES", DATES, problems.isEmpty(), detail.toString());
  }

  private static ValidationCheck testPremium(int seq, ProductVersion version, Context context) {
    if (context.testPremium() == null) {
      String why =
          version.getScheme().defaultRate() == null
              ? "Not computed: rated per insurer, no package rate"
              : "Not computed for this product";
      return new ValidationCheck(seq, "TEST_PREMIUM", TEST_PREMIUM, Result.NOT_APPLICABLE, why);
    }
    String detail =
        DisplayFormat.amount(context.testPremium())
            + " on a sum insured of "
            + DisplayFormat.amount(context.sampleSumInsured())
            + " at "
            + percent(version.getScheme().defaultRate())
            + ", minimum premium "
            + DisplayFormat.amount(version.getScheme().minimumPremium());
    return check(seq, "TEST_PREMIUM", TEST_PREMIUM, true, detail);
  }

  private static ValidationCheck check(
      int seq, String code, String label, boolean passed, String detail) {
    String text = detail.length() > 500 ? detail.substring(0, 497) + "..." : detail;
    return new ValidationCheck(seq, code, label, passed ? Result.PASSED : Result.FAILED, text);
  }

  private static String percent(BigDecimal value) {
    return DisplayFormat.rate(value) + "%";
  }

  private static String count(long n, String noun) {
    return n + " " + (n == 1 ? noun : noun + "s");
  }

  private static String join(List<String> codes) {
    return String.join(", ", codes);
  }
}
