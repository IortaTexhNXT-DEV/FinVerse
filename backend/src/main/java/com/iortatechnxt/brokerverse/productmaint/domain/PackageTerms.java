package com.iortatechnxt.brokerverse.productmaint.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Package terms as requested by Marketing (BRPM.008) and as proposed after negotiation and
 * requirements (BRPM.010/015): free-form sections, coverages with limits and deductibles, the rate
 * scheme, the package dates and the insurers with their own terms (PMADD01/02). Stored as JSON on
 * the request; the MBS set-up turns the proposed terms into a catalog {@code PackageSpec}. Rates
 * are percentages (12.5 = 12.5 %), amounts have scale 2.
 *
 * @param sections free-form sections (heading, text)
 * @param coverages coverages / perils of the package
 * @param scheme rate scheme
 * @param dates effectivity and package term
 * @param insurers target (requested) or chosen (proposed) insurers
 */
public record PackageTerms(
    List<Section> sections,
    List<CoverageTerm> coverages,
    Scheme scheme,
    Dates dates,
    List<InsurerLine> insurers) {

  /** No terms. */
  public static final PackageTerms EMPTY =
      new PackageTerms(List.of(), List.of(), Scheme.NONE, Dates.NONE, List.of());

  /** Defensive copies; null parts become empty. */
  public PackageTerms {
    sections = sections == null ? List.of() : List.copyOf(sections);
    coverages = coverages == null ? List.of() : List.copyOf(coverages);
    scheme = scheme == null ? Scheme.NONE : scheme;
    dates = dates == null ? Dates.NONE : dates;
    insurers = insurers == null ? List.of() : List.copyOf(insurers);
  }

  /**
   * The insurer codes, in order.
   *
   * @return codes
   */
  public List<String> insurerCodes() {
    return insurers.stream().map(InsurerLine::insurerCode).toList();
  }

  /**
   * The same terms with other insurers.
   *
   * @param lines insurers
   * @return terms
   */
  public PackageTerms withInsurers(List<InsurerLine> lines) {
    return new PackageTerms(sections, coverages, scheme, dates, lines);
  }

  /**
   * The same terms with another rate scheme and dates.
   *
   * @param newScheme rate scheme
   * @param newDates dates
   * @return terms
   */
  public PackageTerms withScheme(Scheme newScheme, Dates newDates) {
    return new PackageTerms(sections, coverages, newScheme, newDates, insurers);
  }

  /**
   * A free-form section of the requested terms.
   *
   * @param heading heading
   * @param text text
   */
  public record Section(String heading, String text) {}

  /**
   * Terms of one coverage / peril, for the package or for one insurer (PMADD01/02).
   *
   * @param coverageCode coverage code (catalog coverage master)
   * @param included included
   * @param optional optional for the client
   * @param limitAmount limit, null when none
   * @param subLimit sub-limit, null when none
   * @param deductibleAmount deductible amount, null when none
   * @param deductiblePercent deductible percent, null when none
   * @param deductibleText deductible wording, null when none
   * @param clauseCodes warranties, clauses and exclusions (catalog clause codes)
   * @param remarks remarks
   */
  public record CoverageTerm(
      String coverageCode,
      boolean included,
      boolean optional,
      BigDecimal limitAmount,
      BigDecimal subLimit,
      BigDecimal deductibleAmount,
      BigDecimal deductiblePercent,
      String deductibleText,
      List<String> clauseCodes,
      String remarks) {

    /** Defensive copy. */
    public CoverageTerm {
      clauseCodes = clauseCodes == null ? List.of() : List.copyOf(clauseCodes);
    }
  }

  /**
   * The package rate scheme (BRPM.007/015).
   *
   * @param defaultRate premium rate in percent
   * @param minimumPremium minimum premium
   * @param commissionRate commission rate in percent
   * @param maxSumInsured package TSI limit
   * @param ratingBasisNote computation basis agreed with the insurers (PQ12)
   */
  public record Scheme(
      BigDecimal defaultRate,
      BigDecimal minimumPremium,
      BigDecimal commissionRate,
      BigDecimal maxSumInsured,
      String ratingBasisNote) {

    /** No scheme yet. */
    public static final Scheme NONE = new Scheme(null, null, null, null, null);
  }

  /**
   * Effectivity and term of the package (BRPM.017).
   *
   * @param effectiveFrom date the version sells from
   * @param packageStartDate start of the insurer agreement
   * @param packageEndDate end of the insurer agreement
   * @param anniversaryDate anniversary date, null when none (PQ11)
   */
  public record Dates(
      LocalDate effectiveFrom,
      LocalDate packageStartDate,
      LocalDate packageEndDate,
      LocalDate anniversaryDate) {

    /** No dates yet. */
    public static final Dates NONE = new Dates(null, null, null, null);
  }

  /**
   * An insurer of the package with its role and own terms (PMADD02, PQ03).
   *
   * @param insurerCode insurer party code
   * @param role LEAD, PARTICIPANT or PANEL (null = PANEL)
   * @param sharePercent co-insurance share, null for a panel insurer
   * @param rate insurer rate in percent, null for the scheme rate
   * @param minimumPremium insurer minimum premium, null for the scheme minimum
   * @param terms insurer terms per coverage
   */
  public record InsurerLine(
      String insurerCode,
      String role,
      BigDecimal sharePercent,
      BigDecimal rate,
      BigDecimal minimumPremium,
      List<CoverageTerm> terms) {

    /** Defensive copy. */
    public InsurerLine {
      terms = terms == null ? List.of() : List.copyOf(terms);
    }

    /**
     * A panel insurer without own terms (target insurer of a request).
     *
     * @param code insurer code
     * @return line
     */
    public static InsurerLine target(String code) {
      return new InsurerLine(code, null, null, null, null, List.of());
    }
  }
}
