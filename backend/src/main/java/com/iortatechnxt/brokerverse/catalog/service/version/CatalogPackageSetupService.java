package com.iortatechnxt.brokerverse.catalog.service.version;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.PaymentGate;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLifecycle;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersion;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionRepository;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct.ProductDetails;
import com.iortatechnxt.brokerverse.catalog.domain.TsuInvolvement;
import com.iortatechnxt.brokerverse.catalog.service.CoverageService;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalog implementation of {@link PackageSetupService} (BRPM.015, PMADD01/02/06;
 * PRODUCT_MAINTENANCE_DESIGN sections 5.1 and 9.1): turns negotiated package terms, or the "New
 * Version" of the catalog editor, into a DRAFT version. It replaces the in-memory stub of {@link
 * PackageVersionStubDefaults}. Every reference is checked (line coverages, clause library, insurer
 * panel), one version at a time is set up per product, and every change is audited (BRPM.024).
 */
@Service
@Primary
@Transactional
public class CatalogPackageSetupService implements PackageSetupService {

  /** Permission of the package set-up (MBS). */
  static final String MAINTAIN = "PRODUCT_MAINTAIN";

  private static final String PRODUCT = "Product ";

  private final ProductVersionRepository versions;
  private final ProductCatalogService catalog;
  private final CoverageService coverages;
  private final InsurerService insurers;
  private final PackageIncentiveReview incentives;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param versions versions
   * @param catalog products and lines
   * @param coverages coverages and clauses
   * @param insurers insurer panel
   * @param incentives review of incentive criteria of retired products (PMADD08)
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public CatalogPackageSetupService(
      ProductVersionRepository versions,
      ProductCatalogService catalog,
      CoverageService coverages,
      InsurerService insurers,
      PackageIncentiveReview incentives,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.versions = versions;
    this.catalog = catalog;
    this.coverages = coverages;
    this.insurers = insurers;
    this.incentives = incentives;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @Override
  public VersionRef createDraftVersion(PackageSpec spec) {
    requireMaintainer();
    RiskProduct product = productFor(spec);
    List<ProductVersion> existing =
        versions.findByProductCodeOrderByVersionNoDesc(product.getCode());
    if (existing.stream().anyMatch(ProductVersion::isOpen)) {
      throw new BusinessRuleException(
          "VERSION_IN_PROGRESS",
          PRODUCT + product.getCode() + " already has a version being set up");
    }
    validate(product, spec);
    int next = existing.stream().mapToInt(ProductVersion::getVersionNo).max().orElse(0) + 1;
    ProductVersion saved =
        versions.save(
            new ProductVersion(
                product.getCode(),
                next,
                PackageVersionMapper.content(spec, false),
                PackageVersionMapper.origin(spec.origin())));
    audit.record(
        ProductVersionQueries.ENTITY,
        saved.reference(),
        AuditAction.CREATE,
        "Draft version " + next + originText(spec.origin()));
    return ref(saved);
  }

  @Override
  public VersionRef updateDraftVersion(String productCode, int versionNo, PackageSpec spec) {
    requireMaintainer();
    if (!productCode.equals(spec.productCode())) {
      throw new BusinessRuleException(
          "VERSION_PRODUCT_MISMATCH", "The content is for product " + spec.productCode());
    }
    RiskProduct product = catalog.requireProduct(productCode);
    ProductVersion version =
        versions
            .findByProductCodeAndVersionNo(productCode, versionNo)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ProductVersionQueries.ENTITY, productCode + " v" + versionNo));
    validate(product, spec);
    version.replace(
        PackageVersionMapper.content(spec, version.getScheme().manualRateAllowed()),
        PackageVersionMapper.origin(spec.origin()));
    audit.record(
        ProductVersionQueries.ENTITY,
        version.reference(),
        AuditAction.UPDATE,
        "Draft content replaced" + originText(spec.origin()));
    return ref(version);
  }

  /**
   * "New Version" of the catalog editor: a DRAFT copied from the version in force, effective
   * tomorrow, for MBS to change (PRODUCT_MAINTENANCE_DESIGN section 5.1).
   *
   * @param companyId company (insurer look-ups)
   * @param productCode risk code of a versioned package
   * @param changeSummary what is going to change
   * @return the new DRAFT version
   */
  public VersionRef draftFromCurrent(Long companyId, String productCode, String changeSummary) {
    ProductVersion base =
        versions.findByProductCodeOrderByVersionNoDesc(productCode).stream()
            .filter(v -> !v.isOpen())
            .max(Comparator.comparing(ProductVersion::getEffectiveFrom))
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "PRODUCT_NOT_VERSIONED", PRODUCT + productCode + " has no version"));
    PackageSpec copy =
        PackageVersionMapper.specOf(
            companyId, base, new PackageSpec.Origin(null, null, changeSummary));
    LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
    PackageSpec.PackageDates d = copy.dates();
    PackageSpec spec =
        new PackageSpec(
            copy.companyId(),
            copy.productCode(),
            null,
            copy.baseVersionNo(),
            copy.rateScheme(),
            new PackageSpec.PackageDates(
                tomorrow, d.packageStartDate(), d.packageEndDate(), d.anniversaryDate()),
            copy.coverages(),
            copy.insurers(),
            copy.insurerTerms(),
            copy.origin());
    return createDraftVersion(spec);
  }

  @Override
  public void retireProduct(String productCode, PackageSpec.Origin origin) {
    requireMaintainer();
    RiskProduct product = catalog.requireProduct(productCode);
    if (product.getLifecycleStatus() != ProductLifecycle.ACTIVE) {
      throw new BusinessRuleException(
          "PRODUCT_NOT_ACTIVE",
          PRODUCT + productCode + " is " + product.getLifecycleStatus() + " already");
    }
    product.changeLifecycle(ProductLifecycle.RETIRED);
    audit.record(
        PRODUCT.strip(),
        productCode,
        AuditAction.UPDATE,
        "Retired (BRPM.011)" + originText(origin));
    incentives.productInactive(productCode, "retired");
  }

  private RiskProduct productFor(PackageSpec spec) {
    if (spec.productCode() == null || spec.productCode().isBlank()) {
      throw new BusinessRuleException("PRODUCT_CODE_REQUIRED", "Enter the product code");
    }
    if (spec.newProduct() == null) {
      RiskProduct product = catalog.requireProduct(spec.productCode());
      if (!product.isPackaged()) {
        throw new BusinessRuleException(
            "PRODUCT_NOT_PACKAGED", PRODUCT + spec.productCode() + " is not a package");
      }
      return product;
    }
    if (versions.existsByProductCode(spec.productCode())) {
      throw new DuplicateResourceException("Product", spec.productCode());
    }
    return createPackage(spec);
  }

  private RiskProduct createPackage(PackageSpec spec) {
    PackageSpec.NewProduct n = spec.newProduct();
    PackageSpec.RateScheme r = spec.rateScheme();
    return catalog.createProduct(
        spec.productCode(),
        new ProductDetails(
            n.name(),
            n.lineCode(),
            n.coverTypeCode(),
            true,
            false,
            n.marketSegments(),
            false,
            false,
            false,
            1,
            false,
            PaymentGate.CLIENT_CONFIRMATION,
            r == null ? null : r.defaultRate(),
            r == null || r.defaultCommissionRate() == null
                ? BigDecimal.ZERO
                : r.defaultCommissionRate(),
            r == null || r.minimumPremium() == null ? BigDecimal.ZERO : r.minimumPremium(),
            r == null ? null : r.maxSumInsured(),
            TsuInvolvement.BY_RULES));
  }

  private void validate(RiskProduct product, PackageSpec spec) {
    String line = product.getLineCode();
    spec.coverages().forEach(c -> coverages.requireCoverage(line, c.coverageCode()));
    spec.insurers().forEach(i -> insurers.requireInsurer(spec.companyId(), i.insurerCode()));
    for (PackageSpec.InsurerTerm t : spec.insurerTerms()) {
      coverages.requireCoverage(line, t.coverageCode());
      if (spec.insurers().stream().noneMatch(i -> i.insurerCode().equals(t.insurerCode()))) {
        throw new BusinessRuleException(
            "PACKAGE_TERM_INSURER_UNKNOWN",
            "Insurer " + t.insurerCode() + " has terms but is not on the package");
      }
      t.clauseCodes().forEach(this::requireClauseOf);
    }
  }

  private void requireClauseOf(String code) {
    coverages.requireClause(code);
  }

  private void requireMaintainer() {
    if (!currentUser.hasAuthority(MAINTAIN)) {
      throw new AccessDeniedException("Package set-up needs " + MAINTAIN);
    }
  }

  private static VersionRef ref(ProductVersion v) {
    return new VersionRef(
        v.getProductCode(), v.getVersionNo(), v.getStatus(), v.getEffectiveFrom());
  }

  private static String originText(PackageSpec.Origin origin) {
    if (origin == null || origin.sourceRequestNo() == null) {
      return "";
    }
    return " from request " + origin.sourceRequestNo();
  }
}
