package com.iortatechnxt.brokerverse.catalog.service.version;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.Coverage;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLifecycle;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersion;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionRepository;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionStatus;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.CoverageService;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.RatingService;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The validation checkpoint and lifecycle of package versions (PMADD06, BRPM.006/007/017;
 * PRODUCT_MAINTENANCE_DESIGN section 5.1): MBS submits a complete DRAFT; a PRODUCT_VALIDATE holder
 * who is neither its maker nor its submitter confirms the checklist, sees a test premium and
 * releases it, or returns it with a reason. Release ends the previous version the day before,
 * projects the scheme on the product columns once effective and publishes {@link
 * ProductVersionReleased}; a return publishes {@link ProductVersionReturned}. The daily step {@link
 * #expireDue} supersedes ended versions and expires packages whose end date passed ({@link
 * ProductExpired}). Every action is audited.
 */
@Service
@Transactional
public class ProductVersionService {

  /** Permission of the validation checkpoint (TSU Head / Business Administrator, PQ09). */
  public static final String VALIDATE = "PRODUCT_VALIDATE";

  private static final BigDecimal SAMPLE_SUM_INSURED = new BigDecimal("1000000.00");

  private final ProductVersionRepository versions;
  private final ProductVersionQueries queries;
  private final ProductCatalogService catalog;
  private final CoverageService coverages;
  private final RatingService rating;
  private final PackageIncentiveReview incentives;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final ApplicationEventPublisher events;
  private final ObjectMapper json;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param versions versions
   * @param queries version look-ups
   * @param catalog products
   * @param coverages coverages (basic flag)
   * @param rating rating (test premium)
   * @param incentives incentive criteria review (PMADD08)
   * @param audit audit trail
   * @param currentUser current user
   * @param events event publisher
   * @param json JSON (checklist)
   * @param clock clock
   */
  public ProductVersionService(
      ProductVersionRepository versions,
      ProductVersionQueries queries,
      ProductCatalogService catalog,
      CoverageService coverages,
      RatingService rating,
      PackageIncentiveReview incentives,
      AuditTrailService audit,
      CurrentUser currentUser,
      ApplicationEventPublisher events,
      ObjectMapper json,
      Clock clock) {
    this.versions = versions;
    this.queries = queries;
    this.catalog = catalog;
    this.coverages = coverages;
    this.rating = rating;
    this.incentives = incentives;
    this.audit = audit;
    this.currentUser = currentUser;
    this.events = events;
    this.json = json;
    this.clock = clock;
  }

  /**
   * Submits a complete DRAFT for validation (MBS, PRODUCT_MAINTAIN).
   *
   * @param productCode risk code
   * @param versionNo version
   * @return the version, FOR_VALIDATION
   */
  public ProductVersion submitForValidation(String productCode, int versionNo) {
    requirePermission(CatalogPackageSetupService.MAINTAIN);
    ProductVersion version = queries.require(productCode, versionNo);
    RiskProduct product = catalog.requireProduct(productCode);
    checkComplete(product, version);
    version.submit(currentUser.username(), clock.instant());
    audit.record(
        ProductVersionQueries.ENTITY,
        version.reference(),
        AuditAction.SUBMIT,
        "Submitted for validation");
    return version;
  }

  /**
   * Validates and releases a submitted version (PMADD06).
   *
   * @param productCode risk code
   * @param versionNo version
   * @param checklist checklist items the validator confirmed
   * @return the version, RELEASED
   */
  public ProductVersion validate(String productCode, int versionNo, List<String> checklist) {
    requirePermission(VALIDATE);
    ProductVersion version = queries.require(productCode, versionNo);
    String user = currentUser.username();
    if (CurrentUser.sameUser(user, version.getCreatedBy())
        || CurrentUser.sameUser(user, version.getSubmittedBy())) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION",
          "A package version is validated by someone other than its maker and submitter");
    }
    if (version.getStatus() != ProductVersionStatus.FOR_VALIDATION) {
      throw new BusinessRuleException(
          "VERSION_NOT_FOR_VALIDATION", "Version " + versionNo + " is " + version.getStatus());
    }
    RiskProduct product = catalog.requireProduct(productCode);
    checkComplete(product, version);
    LocalDate today = LocalDate.now(clock);
    endPrevious(version, today);
    BigDecimal testPremium =
        rating
            .testPremium(productCode, version.getScheme(), sampleSumInsured(version))
            .orElse(null);
    Instant now = clock.instant();
    version.release(user, now, checklistJson(checklist), testPremium);
    if (!version.getEffectiveFrom().isAfter(today)) {
      product.projectScheme(version.getScheme());
    }
    if (product.getRecordStatus() == RecordStatus.PENDING_AUTHORIZATION) {
      product.authorize(user, now);
    }
    audit.record(
        ProductVersionQueries.ENTITY,
        version.reference(),
        AuditAction.AUTHORIZE,
        "Validated and released, effective " + version.getEffectiveFrom());
    events.publishEvent(
        new ProductVersionReleased(
            productCode,
            versionNo,
            version.getEffectiveFrom(),
            version.getSourceRequestNo(),
            user));
    return version;
  }

  /**
   * Returns a submitted version to DRAFT with a reason (PMADD06).
   *
   * @param productCode risk code
   * @param versionNo version
   * @param reason reason
   * @return the version, DRAFT
   */
  public ProductVersion returnToDraft(String productCode, int versionNo, String reason) {
    requirePermission(VALIDATE);
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("REASON_REQUIRED", "Enter the reason of the return");
    }
    ProductVersion version = queries.require(productCode, versionNo);
    version.returnToDraft(reason.strip());
    audit.record(
        ProductVersionQueries.ENTITY,
        version.reference(),
        AuditAction.REJECT,
        "Returned to MBS: " + reason.strip());
    events.publishEvent(
        new ProductVersionReturned(
            productCode,
            versionNo,
            reason.strip(),
            version.getSourceRequestNo(),
            currentUser.username()));
    return version;
  }

  /**
   * The daily lifecycle step (BRPM.006/017; run by the PACKAGE_EXPIRY_MONITOR job of {@code
   * productmaint}): versions whose selling period ended become SUPERSEDED, a version that took
   * effect is projected on its product, and a current version whose package end date passed becomes
   * EXPIRED with its product ({@link ProductExpired}, incentive criteria flagged).
   *
   * @param date business date
   * @return what changed
   */
  public ExpiryOutcome expireDue(LocalDate date) {
    int superseded = 0;
    int projected = 0;
    int expired = 0;
    List<ProductVersion> released =
        versions.findByStatusInOrderBySubmittedAtAscIdAsc(List.of(ProductVersionStatus.RELEASED));
    for (ProductVersion v : released) {
      if (v.supersedeIfEnded(date)) {
        superseded++;
      } else if (v.getEffectiveTo() == null
          && v.getPackageEndDate() != null
          && v.getPackageEndDate().isBefore(date)) {
        expire(v);
        expired++;
      } else if (v.isInForce(date) && project(v)) {
        projected++;
      }
    }
    if (superseded + projected + expired > 0) {
      audit.record(
          ProductVersionQueries.ENTITY,
          date.toString(),
          AuditAction.UPDATE,
          "Daily step: " + superseded + " superseded, " + expired + " expired");
    }
    return new ExpiryOutcome(superseded, projected, expired);
  }

  private void expire(ProductVersion v) {
    v.expire();
    RiskProduct product = catalog.requireProduct(v.getProductCode());
    product.changeLifecycle(ProductLifecycle.EXPIRED);
    audit.record(
        ProductVersionQueries.ENTITY,
        v.reference(),
        AuditAction.UPDATE,
        "Expired: package end date " + v.getPackageEndDate() + " passed");
    events.publishEvent(
        new ProductExpired(v.getProductCode(), v.getVersionNo(), v.getPackageEndDate()));
    incentives.productInactive(v.getProductCode(), "expired");
  }

  private boolean project(ProductVersion v) {
    RiskProduct product = catalog.requireProduct(v.getProductCode());
    if (!product.changesVersionedColumns(detailsOf(product, v))) {
      return false;
    }
    product.projectScheme(v.getScheme());
    return true;
  }

  private static RiskProduct.ProductDetails detailsOf(RiskProduct p, ProductVersion v) {
    return new RiskProduct.ProductDetails(
        p.getName(),
        p.getLineCode(),
        p.getCoverTypeCode(),
        p.isPackaged(),
        p.isFleetCapable(),
        p.getMarketSegmentList(),
        p.isMortgageApplicable(),
        p.isDirectPaymentEligible(),
        p.isMultiYearAllowed(),
        p.getMaxTermYears(),
        p.isFfyEligible(),
        p.getPaymentGate(),
        v.getScheme().defaultRate(),
        v.getScheme().defaultCommissionRate(),
        v.getScheme().minimumPremium(),
        v.getScheme().maxSumInsured(),
        p.getTsuInvolvement());
  }

  private void checkComplete(RiskProduct product, ProductVersion version) {
    Set<String> basic =
        coverages.coverages(product.getLineCode()).stream()
            .filter(Coverage::isBasic)
            .map(Coverage::getCode)
            .collect(Collectors.toSet());
    PackageCompleteness.check(
        product, version, basic::contains, latestReleased(version), LocalDate.now(clock));
  }

  private ProductVersion latestReleased(ProductVersion version) {
    return queries.entities(version.getProductCode()).stream()
        .filter(v -> v.getVersionNo() != version.getVersionNo())
        .filter(
            v ->
                v.getStatus() == ProductVersionStatus.RELEASED
                    || v.getStatus() == ProductVersionStatus.SUPERSEDED)
        .max(Comparator.comparing(ProductVersion::getEffectiveFrom))
        .orElse(null);
  }

  private void endPrevious(ProductVersion version, LocalDate today) {
    queries.entities(version.getProductCode()).stream()
        .filter(v -> v.getStatus() == ProductVersionStatus.RELEASED && v.getEffectiveTo() == null)
        .forEach(
            previous -> {
              previous.endBefore(version.getEffectiveFrom());
              previous.supersedeIfEnded(today);
              versions.saveAndFlush(previous);
            });
  }

  private static BigDecimal sampleSumInsured(ProductVersion v) {
    BigDecimal limit = v.getScheme().maxSumInsured();
    return limit != null && limit.compareTo(SAMPLE_SUM_INSURED) < 0 ? limit : SAMPLE_SUM_INSURED;
  }

  private String checklistJson(List<String> checklist) {
    try {
      return json.writeValueAsString(checklist == null ? List.of() : checklist);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Checklist cannot be written", e);
    }
  }

  private void requirePermission(String permission) {
    if (!currentUser.hasAuthority(permission)) {
      throw new AccessDeniedException("This action needs " + permission);
    }
  }

  /**
   * Result of the daily lifecycle step.
   *
   * @param superseded versions superseded
   * @param projected versions that took effect and were projected on their product
   * @param expired packages expired
   */
  public record ExpiryOutcome(int superseded, int projected, int expired) {}
}
