package com.iortatechnxt.brokerverse.catalog.service.version;

import com.iortatechnxt.brokerverse.catalog.domain.ProductVersion;
import com.iortatechnxt.brokerverse.catalog.domain.SchemeTerms;
import com.iortatechnxt.brokerverse.catalog.domain.VersionCoverage;
import com.iortatechnxt.brokerverse.catalog.domain.VersionInsurer;
import com.iortatechnxt.brokerverse.catalog.domain.VersionInsurerTerm;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSpec.Deductible;
import java.util.List;

/**
 * Converts between the package set-up contract ({@link PackageSpec}, {@link ProductVersionView})
 * and the catalog version entity (PRODUCT_MAINTENANCE_DESIGN sections 4.2 and 9.1). Pure mapping,
 * no validation.
 */
public final class PackageVersionMapper {

  private PackageVersionMapper() {}

  /**
   * The entity content of a spec.
   *
   * @param spec package spec
   * @param manualRateAllowed whether item rates other than the scheme rate are accepted (PQ10)
   * @return content
   */
  public static ProductVersion.Content content(PackageSpec spec, boolean manualRateAllowed) {
    PackageSpec.RateScheme r = spec.rateScheme();
    PackageSpec.PackageDates d = spec.dates();
    SchemeTerms scheme =
        r == null
            ? new SchemeTerms(null, null, null, null, null, manualRateAllowed)
            : new SchemeTerms(
                r.defaultRate(),
                r.minimumPremium(),
                r.defaultCommissionRate(),
                r.maxSumInsured(),
                r.ratingBasisNote(),
                manualRateAllowed);
    return new ProductVersion.Content(
        d == null ? null : d.effectiveFrom(),
        d == null ? null : d.packageStartDate(),
        d == null ? null : d.packageEndDate(),
        d == null ? null : d.anniversaryDate(),
        scheme,
        spec.coverages().stream().map(PackageVersionMapper::coverage).toList(),
        spec.insurers().stream().map(i -> insurer(spec.companyId(), i)).toList(),
        spec.insurerTerms().stream().map(PackageVersionMapper::term).toList());
  }

  /**
   * The entity origin of a spec.
   *
   * @param origin spec origin, may be null
   * @return origin
   */
  public static ProductVersion.Origin origin(PackageSpec.Origin origin) {
    return origin == null
        ? new ProductVersion.Origin(null, null, null)
        : new ProductVersion.Origin(
            origin.sourceRequestNo(), origin.mancomSignoffRef(), origin.changeSummary());
  }

  /**
   * The read model of a version.
   *
   * @param v version
   * @param productName product name
   * @return view
   */
  public static ProductVersionView view(ProductVersion v, String productName) {
    SchemeTerms s = v.getScheme();
    return new ProductVersionView(
        v.getProductCode(),
        productName,
        v.getVersionNo(),
        v.getStatus(),
        new PackageSpec.PackageDates(
            v.getEffectiveFrom(),
            v.getPackageStartDate(),
            v.getPackageEndDate(),
            v.getAnniversaryDate()),
        v.getEffectiveTo(),
        new PackageSpec.RateScheme(
            s.defaultRate(),
            s.minimumPremium(),
            s.defaultCommissionRate(),
            s.maxSumInsured(),
            s.ratingBasisNote()),
        v.getCoverages().stream().map(PackageVersionMapper::coverage).toList(),
        v.getInsurers().stream().map(PackageVersionMapper::insurer).toList(),
        v.getInsurerTerms().stream().map(PackageVersionMapper::term).toList(),
        new PackageSpec.Origin(
            v.getSourceRequestNo(), v.getMancomSignoffRef(), v.getChangeSummary()),
        new ProductVersionView.Checkpoint(
            v.getSubmittedBy(),
            v.getSubmittedAt(),
            v.getValidatedBy(),
            v.getValidatedAt(),
            v.getReturnedReason()));
  }

  /**
   * The spec of an existing version (base of the next draft).
   *
   * @param companyId company
   * @param v version
   * @param origin origin of the new draft
   * @return spec with the version's content
   */
  public static PackageSpec specOf(Long companyId, ProductVersion v, PackageSpec.Origin origin) {
    ProductVersionView view = view(v, null);
    return new PackageSpec(
        companyId,
        v.getProductCode(),
        null,
        v.getVersionNo(),
        view.rateScheme(),
        view.dates(),
        view.coverages(),
        view.insurers(),
        view.insurerTerms(),
        origin);
  }

  private static VersionCoverage coverage(PackageSpec.Coverage c) {
    Deductible d = deductible(c.deductible());
    return new VersionCoverage(
        c.coverageCode(),
        c.included(),
        c.optional(),
        c.limitAmount(),
        c.subLimit(),
        d.amount(),
        d.percent(),
        d.text(),
        c.sortOrder());
  }

  private static PackageSpec.Coverage coverage(VersionCoverage c) {
    return new PackageSpec.Coverage(
        c.coverageCode(),
        c.included(),
        c.optional(),
        c.limitAmount(),
        c.subLimit(),
        new Deductible(c.deductibleAmount(), c.deductiblePercent(), c.deductibleText()),
        c.sortOrder());
  }

  private static VersionInsurer insurer(Long companyId, PackageSpec.Insurer i) {
    return new VersionInsurer(
        companyId,
        i.insurerCode(),
        i.role(),
        i.sharePercent(),
        i.rate(),
        i.minimumPremium(),
        i.defaultBranchCode());
  }

  private static PackageSpec.Insurer insurer(VersionInsurer i) {
    return new PackageSpec.Insurer(
        i.insurerCode(),
        i.role(),
        i.sharePercent(),
        i.rate(),
        i.minimumPremium(),
        i.defaultBranchCode());
  }

  private static VersionInsurerTerm term(PackageSpec.InsurerTerm t) {
    Deductible d = deductible(t.deductible());
    return new VersionInsurerTerm(
        t.insurerCode(),
        t.coverageCode(),
        t.included(),
        t.limitAmount(),
        t.subLimit(),
        d.amount(),
        d.percent(),
        d.text(),
        t.clauseCodes().isEmpty() ? null : String.join(",", t.clauseCodes()),
        t.remarks());
  }

  private static PackageSpec.InsurerTerm term(VersionInsurerTerm t) {
    return new PackageSpec.InsurerTerm(
        t.insurerCode(),
        t.coverageCode(),
        t.included(),
        t.limitAmount(),
        t.subLimit(),
        new Deductible(t.deductibleAmount(), t.deductiblePercent(), t.deductibleText()),
        List.copyOf(t.clauseCodeList()),
        t.remarks());
  }

  private static Deductible deductible(Deductible d) {
    return d == null ? new Deductible(null, null, null) : d;
  }
}
