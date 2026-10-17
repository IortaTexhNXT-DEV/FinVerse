package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.catalog.domain.PackageInsurerRole;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSpec;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSpec.Coverage;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSpec.Deductible;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSpec.Insurer;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSpec.InsurerTerm;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionView;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.CoverageTerm;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.Dates;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.InsurerLine;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.Scheme;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maps between the request's package terms and the catalog contracts (BRPM.011/015/017): a catalog
 * version pre-fills an AMEND / RENEW request (no re-keying), and the proposed terms become the
 * {@link PackageSpec} of the MBS set-up. Insurers without own coverage terms get the package
 * coverages as their terms, so that every panel insurer has a term per included coverage (PMADD02).
 */
final class VersionTerms {

  private static final int SORT_STEP = 10;

  private VersionTerms() {}

  /**
   * The terms of a catalog version.
   *
   * @param v version
   * @return terms
   */
  static PackageTerms of(ProductVersionView v) {
    List<CoverageTerm> coverages = v.coverages().stream().map(VersionTerms::coverage).toList();
    List<InsurerLine> insurers =
        v.insurers().stream()
            .map(
                i ->
                    new InsurerLine(
                        i.insurerCode(),
                        i.role() == null ? null : i.role().name(),
                        i.sharePercent(),
                        i.rate(),
                        i.minimumPremium(),
                        v.insurerTerms().stream()
                            .filter(t -> t.insurerCode().equals(i.insurerCode()))
                            .map(VersionTerms::insurerCoverage)
                            .toList()))
            .toList();
    PackageSpec.RateScheme r = v.rateScheme();
    Scheme scheme =
        r == null
            ? Scheme.NONE
            : new Scheme(
                r.defaultRate(),
                r.minimumPremium(),
                r.defaultCommissionRate(),
                r.maxSumInsured(),
                r.ratingBasisNote());
    PackageSpec.PackageDates d = v.dates();
    Dates dates =
        d == null
            ? Dates.NONE
            : new Dates(
                d.effectiveFrom(), d.packageStartDate(), d.packageEndDate(), d.anniversaryDate());
    return new PackageTerms(List.of(), coverages, scheme, dates, insurers);
  }

  private static CoverageTerm coverage(Coverage c) {
    Deductible d = c.deductible() == null ? new Deductible(null, null, null) : c.deductible();
    return new CoverageTerm(
        c.coverageCode(),
        c.included(),
        c.optional(),
        c.limitAmount(),
        c.subLimit(),
        d.amount(),
        d.percent(),
        d.text(),
        List.of(),
        null);
  }

  private static CoverageTerm insurerCoverage(InsurerTerm t) {
    Deductible d = t.deductible() == null ? new Deductible(null, null, null) : t.deductible();
    return new CoverageTerm(
        t.coverageCode(),
        t.included(),
        false,
        t.limitAmount(),
        t.subLimit(),
        d.amount(),
        d.percent(),
        d.text(),
        t.clauseCodes(),
        t.remarks());
  }

  /**
   * The package spec of the MBS set-up.
   *
   * @param setup set-up facts (company, product, base version, origin)
   * @param terms proposed terms
   * @return spec
   */
  static PackageSpec spec(SetupFacts setup, PackageTerms terms) {
    List<Coverage> coverages = new ArrayList<>();
    for (int i = 0; i < terms.coverages().size(); i++) {
      CoverageTerm c = terms.coverages().get(i);
      coverages.add(
          new Coverage(
              c.coverageCode(),
              c.included(),
              c.optional(),
              c.limitAmount(),
              c.subLimit(),
              deductible(c),
              (i + 1) * SORT_STEP));
    }
    List<Insurer> insurers = terms.insurers().stream().map(VersionTerms::insurer).toList();
    List<InsurerTerm> insurerTerms = new ArrayList<>();
    for (InsurerLine line : terms.insurers()) {
      List<CoverageTerm> own =
          line.terms().isEmpty()
              ? terms.coverages().stream().filter(CoverageTerm::included).toList()
              : line.terms();
      own.forEach(t -> insurerTerms.add(insurerTerm(line.insurerCode(), t)));
    }
    Scheme s = terms.scheme();
    Dates d = terms.dates();
    return new PackageSpec(
        setup.companyId(),
        setup.productCode(),
        setup.newProduct(),
        setup.baseVersionNo(),
        new PackageSpec.RateScheme(
            s.defaultRate(),
            s.minimumPremium(),
            s.commissionRate(),
            s.maxSumInsured(),
            s.ratingBasisNote()),
        new PackageSpec.PackageDates(
            d.effectiveFrom(), d.packageStartDate(), d.packageEndDate(), d.anniversaryDate()),
        coverages,
        insurers,
        insurerTerms,
        setup.origin());
  }

  private static Insurer insurer(InsurerLine line) {
    PackageInsurerRole role =
        line.role() == null || line.role().isBlank()
            ? PackageInsurerRole.PANEL
            : PackageInsurerRole.valueOf(line.role().strip().toUpperCase(Locale.ROOT));
    return new Insurer(
        line.insurerCode(), role, line.sharePercent(), line.rate(), line.minimumPremium(), null);
  }

  private static InsurerTerm insurerTerm(String insurerCode, CoverageTerm t) {
    return new InsurerTerm(
        insurerCode,
        t.coverageCode(),
        t.included(),
        t.limitAmount(),
        t.subLimit(),
        deductible(t),
        t.clauseCodes(),
        t.remarks());
  }

  private static Deductible deductible(CoverageTerm c) {
    boolean none =
        c.deductibleAmount() == null
            && c.deductiblePercent() == null
            && (c.deductibleText() == null || c.deductibleText().isBlank());
    return none
        ? null
        : new Deductible(c.deductibleAmount(), c.deductiblePercent(), c.deductibleText());
  }

  /**
   * What the set-up adds to the proposed terms.
   *
   * @param companyId company
   * @param productCode product code (existing, or the code of a new product)
   * @param newProduct identity of a new product, null otherwise
   * @param baseVersionNo version the draft starts from
   * @param origin request number, ManCom reference and change summary
   */
  record SetupFacts(
      Long companyId,
      String productCode,
      PackageSpec.NewProduct newProduct,
      Integer baseVersionNo,
      PackageSpec.Origin origin) {}
}
