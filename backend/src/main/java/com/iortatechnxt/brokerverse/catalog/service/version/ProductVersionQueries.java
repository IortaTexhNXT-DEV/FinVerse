package com.iortatechnxt.brokerverse.catalog.service.version;

import com.iortatechnxt.brokerverse.catalog.domain.ProductVersion;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionRepository;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionStatus;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProductRepository;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalog implementation of {@link ProductVersionQueryService} (BRPM.006/007/017;
 * PRODUCT_MAINTENANCE_DESIGN sections 5.1, 5.2 and 9.1). The version in force is decided by the
 * selling period (effective from / to) of the RELEASED and SUPERSEDED versions, so rating does not
 * depend on the daily job having run. Also gives rating and the version screens the entities.
 *
 * <p>The views of the port are cached ({@link CatalogCaches#PRODUCT_VERSIONS}); the entity methods
 * are not.
 */
@Service
@Primary
@Transactional(readOnly = true)
public class ProductVersionQueries implements ProductVersionQueryService {

  /** Entity label in errors and the audit trail. */
  public static final String ENTITY = "Package version";

  private final ProductVersionRepository versions;
  private final RiskProductRepository products;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param versions versions
   * @param products products (names)
   * @param clock clock (business date)
   */
  public ProductVersionQueries(
      ProductVersionRepository versions, RiskProductRepository products, Clock clock) {
    this.versions = versions;
    this.products = products;
    this.clock = clock;
  }

  @Override
  @Cacheable(
      cacheNames = CatalogCaches.PRODUCT_VERSIONS,
      key = "'current:' + #productCode + ':' + T(java.time.LocalDate).now(@clock)")
  public Optional<ProductVersionView> current(String productCode) {
    return inForce(productCode, LocalDate.now(clock));
  }

  @Override
  @Cacheable(
      cacheNames = CatalogCaches.PRODUCT_VERSIONS,
      key = "'inForce:' + #productCode + ':' + #date")
  public Optional<ProductVersionView> inForce(String productCode, LocalDate date) {
    return inForceEntity(productCode, date).map(this::view);
  }

  @Override
  @Cacheable(
      cacheNames = CatalogCaches.PRODUCT_VERSIONS,
      key = "'version:' + #productCode + ':' + #versionNo")
  public Optional<ProductVersionView> version(String productCode, int versionNo) {
    return versions.findByProductCodeAndVersionNo(productCode, versionNo).map(this::view);
  }

  @Override
  @Cacheable(cacheNames = CatalogCaches.PRODUCT_VERSIONS, key = "'versions:' + #productCode")
  public List<ProductVersionView> versions(String productCode) {
    String name = products.findByCode(productCode).map(RiskProduct::getName).orElse(null);
    return versions.findByProductCodeOrderByVersionNoDesc(productCode).stream()
        .map(v -> PackageVersionMapper.view(v, name))
        .toList();
  }

  @Override
  public List<ProductVersionView> packagesExpiring(Long companyId, int withinDays) {
    LocalDate today = LocalDate.now(clock);
    LocalDate until = today.plusDays(Math.max(0, withinDays));
    return versions
        .findByStatusInOrderBySubmittedAtAscIdAsc(List.of(ProductVersionStatus.RELEASED))
        .stream()
        .filter(v -> v.getEffectiveTo() == null && v.getPackageEndDate() != null)
        .filter(
            v -> !v.getPackageEndDate().isBefore(today) && !v.getPackageEndDate().isAfter(until))
        .sorted(Comparator.comparing(ProductVersion::getPackageEndDate))
        .map(this::view)
        .toList();
  }

  /**
   * The version entity in force on a date.
   *
   * @param productCode risk code
   * @param date date
   * @return version, empty when none
   */
  public Optional<ProductVersion> inForceEntity(String productCode, LocalDate date) {
    return versions.findByProductCodeOrderByVersionNoDesc(productCode).stream()
        .filter(v -> v.isInForce(date))
        .max(Comparator.comparing(ProductVersion::getEffectiveFrom));
  }

  /**
   * One version entity.
   *
   * @param productCode risk code
   * @param versionNo version number
   * @return version
   */
  public ProductVersion require(String productCode, int versionNo) {
    return versions
        .findByProductCodeAndVersionNo(productCode, versionNo)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, productCode + " v" + versionNo));
  }

  /**
   * Whether a product has versions (a versioned package).
   *
   * @param productCode risk code
   * @return true when versioned
   */
  public boolean isVersioned(String productCode) {
    return versions.existsByProductCode(productCode);
  }

  /**
   * Every version entity of a product, newest first.
   *
   * @param productCode risk code
   * @return versions
   */
  public List<ProductVersion> entities(String productCode) {
    return versions.findByProductCodeOrderByVersionNoDesc(productCode);
  }

  /**
   * The versions waiting for validation, oldest submission first (Validation Queue, PMADD06).
   *
   * @return versions FOR_VALIDATION
   */
  public List<ProductVersion> forValidation() {
    return versions.findByStatusInOrderBySubmittedAtAscIdAsc(
        List.of(ProductVersionStatus.FOR_VALIDATION));
  }

  /**
   * The version facts of every versioned product for lists (one query).
   *
   * @return facts by risk code
   */
  public Map<String, VersionFacts> facts() {
    LocalDate today = LocalDate.now(clock);
    Map<String, VersionFacts> result = new HashMap<>();
    versions.findAll().stream()
        .collect(Collectors.groupingBy(ProductVersion::getProductCode))
        .forEach((code, list) -> result.put(code, VersionFacts.of(list, today)));
    return result;
  }

  /**
   * The version facts of one product.
   *
   * @param productCode risk code
   * @return facts, {@link VersionFacts#NONE} for a product without versions
   */
  public VersionFacts facts(String productCode) {
    return VersionFacts.of(entities(productCode), LocalDate.now(clock));
  }

  private ProductVersionView view(ProductVersion v) {
    return PackageVersionMapper.view(
        v, products.findByCode(v.getProductCode()).map(RiskProduct::getName).orElse(null));
  }

  /**
   * What a product list shows about the versions of a package (BRPM.006/007).
   *
   * @param currentVersionNo version in force today, null when none
   * @param currentPackageEndDate package end date of that version, null when none
   * @param openVersionNo version being set up (DRAFT / FOR_VALIDATION), null when none
   * @param openVersionStatus status of that version, null when none
   * @param latestVersionNo highest version number, null for a product without versions
   */
  public record VersionFacts(
      Integer currentVersionNo,
      LocalDate currentPackageEndDate,
      Integer openVersionNo,
      ProductVersionStatus openVersionStatus,
      Integer latestVersionNo) {

    /** A product without versions. */
    public static final VersionFacts NONE = new VersionFacts(null, null, null, null, null);

    static VersionFacts of(List<ProductVersion> list, LocalDate today) {
      if (list.isEmpty()) {
        return NONE;
      }
      ProductVersion current =
          list.stream()
              .filter(v -> v.isInForce(today))
              .max(Comparator.comparing(ProductVersion::getEffectiveFrom))
              .orElse(null);
      ProductVersion open = list.stream().filter(ProductVersion::isOpen).findFirst().orElse(null);
      return new VersionFacts(
          current == null ? null : current.getVersionNo(),
          current == null ? null : current.getPackageEndDate(),
          open == null ? null : open.getVersionNo(),
          open == null ? null : open.getStatus(),
          list.stream().mapToInt(ProductVersion::getVersionNo).max().orElse(0));
    }
  }
}
