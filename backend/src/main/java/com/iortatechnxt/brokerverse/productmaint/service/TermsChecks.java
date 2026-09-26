package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.CoverageTerm;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.Dates;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.InsurerLine;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.Scheme;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Checks on the requested terms of a package request (BRPM.008/021, PMADD01): blank entries
 * removed, no negative rate or amount, a package end date after its start date, and the
 * completeness check before submission.
 */
final class TermsChecks {

  private TermsChecks() {}

  /**
   * The terms without blank sections, blank coverages and duplicate insurers.
   *
   * @param terms terms as entered, may be null
   * @return cleaned terms
   */
  static PackageTerms clean(PackageTerms terms) {
    PackageTerms t = terms == null ? PackageTerms.EMPTY : terms;
    List<PackageTerms.Section> sections =
        t.sections().stream()
            .filter(s -> !blank(s.text()))
            .map(s -> new PackageTerms.Section(strip(s.heading()), s.text().strip()))
            .toList();
    List<CoverageTerm> coverages =
        t.coverages().stream().filter(c -> !blank(c.coverageCode())).toList();
    List<InsurerLine> lines = new ArrayList<>();
    for (InsurerLine line : t.insurers()) {
      if (line.insurerCode() != null
          && lines.stream().noneMatch(l -> l.insurerCode().equals(line.insurerCode()))) {
        lines.add(line);
      }
    }
    return new PackageTerms(sections, coverages, t.scheme(), t.dates(), lines);
  }

  /**
   * Refuses negative rates and amounts and package dates in the wrong order.
   *
   * @param t terms
   */
  static void checkNumbers(PackageTerms t) {
    Scheme s = t.scheme();
    Stream<BigDecimal> amounts =
        Stream.concat(
            Stream.of(s.defaultRate(), s.minimumPremium(), s.commissionRate(), s.maxSumInsured()),
            t.coverages().stream().flatMap(c -> Stream.of(c.limitAmount(), c.subLimit())));
    if (amounts.anyMatch(a -> a != null && a.signum() < 0)) {
      throw new BusinessRuleException(
          "PKG_AMOUNT_NEGATIVE", "Rates and amounts cannot be negative");
    }
    Dates d = t.dates();
    if (d.packageStartDate() != null
        && d.packageEndDate() != null
        && !d.packageEndDate().isAfter(d.packageStartDate())) {
      throw new BusinessRuleException(
          "PKG_DATES_INVALID", "The package end date must be after its start date");
    }
  }

  /**
   * What is still missing before the request can be submitted (BRPM.008/021).
   *
   * @param type request type
   * @param coverTypeCode cover type
   * @param negotiationRequired whether insurers are approached
   * @param terms requested terms
   * @return missing items, empty when complete
   */
  static List<String> missing(
      RequestType type, String coverTypeCode, boolean negotiationRequired, PackageTerms terms) {
    List<String> missing = new ArrayList<>();
    if (type == RequestType.NEW && coverTypeCode == null) {
      missing.add("the cover type (PMADD01)");
    }
    if (negotiationRequired && terms.insurers().isEmpty()) {
      missing.add("at least one target insurer");
    }
    if (type != RequestType.RETIRE) {
      missing.addAll(missingTerms(terms));
    }
    return missing;
  }

  private static List<String> missingTerms(PackageTerms terms) {
    List<String> missing = new ArrayList<>();
    if (terms.coverages().isEmpty() && terms.sections().isEmpty()) {
      missing.add("the requested terms (coverages or sections)");
    }
    if (terms.dates().packageEndDate() == null) {
      missing.add("the package end date");
    }
    return missing;
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static String strip(String value) {
    return blank(value) ? null : value.strip();
  }
}
