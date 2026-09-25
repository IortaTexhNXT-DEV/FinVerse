package com.iortatechnxt.brokerverse.catalog.service.version;

import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <b>Temporary stubs</b> of the package version contracts (PRODUCT_MAINTENANCE_DESIGN section 12,
 * wave P1): registered only while no module provides {@link PackageSetupService} / {@link
 * ProductVersionQueryService}, so that {@code productmaint} (P1-B) runs end to end before the
 * catalog implementation (P1-A) is merged. The catalog replaces each stub by declaring its
 * implementation as a component-scanned {@code @Service}; the integration wave (P2) deletes this
 * class.
 *
 * <p>The stubs keep DRAFT versions in memory (lost on restart), never release anything and never
 * publish events: a released version and the catalog events exist only with the real implementation
 * (tests of {@code productmaint} publish {@link ProductVersionReleased} themselves). Every call
 * logs a warning.
 */
@Configuration(proxyBeanMethods = false)
public class PackageVersionStubDefaults {

  private static final Logger LOG = LoggerFactory.getLogger(PackageVersionStubDefaults.class);

  private final InMemoryVersions versions = new InMemoryVersions();

  /**
   * In-memory package set-up until the catalog implements {@link PackageSetupService}.
   *
   * @return stub
   */
  @Bean
  @ConditionalOnMissingBean(PackageSetupService.class)
  public PackageSetupService stubPackageSetupService() {
    return new StubPackageSetup(versions);
  }

  /**
   * In-memory version reads until the catalog implements {@link ProductVersionQueryService}.
   *
   * @return stub
   */
  @Bean
  @ConditionalOnMissingBean(ProductVersionQueryService.class)
  public ProductVersionQueryService stubProductVersionQueryService() {
    return new StubVersionQuery(versions);
  }

  private static void warn(String call) {
    LOG.warn("Package versions: in-memory stub used for {} (catalog implementation missing)", call);
  }

  /** DRAFT versions created through the stub, per product (newest last). */
  static final class InMemoryVersions {

    private final Map<String, List<ProductVersionView>> byProduct = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    List<ProductVersionView> of(String productCode) {
      lock.lock();
      try {
        return List.copyOf(byProduct.getOrDefault(productCode, List.of()));
      } finally {
        lock.unlock();
      }
    }

    VersionRef add(PackageSpec spec) {
      lock.lock();
      try {
        return addLocked(spec);
      } finally {
        lock.unlock();
      }
    }

    private VersionRef addLocked(PackageSpec spec) {
      List<ProductVersionView> list =
          byProduct.computeIfAbsent(spec.productCode(), k -> new ArrayList<>());
      boolean open =
          list.stream()
              .anyMatch(
                  v ->
                      v.status() == ProductVersionStatus.DRAFT
                          || v.status() == ProductVersionStatus.FOR_VALIDATION);
      if (open) {
        throw new BusinessRuleException(
            "VERSION_IN_PROGRESS",
            "Product " + spec.productCode() + " already has a version being set up");
      }
      ProductVersionView v = draft(spec, list.size() + 1);
      list.add(v);
      return v.ref();
    }

    VersionRef replace(String productCode, int versionNo, PackageSpec spec) {
      lock.lock();
      try {
        return replaceLocked(productCode, versionNo, spec);
      } finally {
        lock.unlock();
      }
    }

    private VersionRef replaceLocked(String productCode, int versionNo, PackageSpec spec) {
      List<ProductVersionView> list = byProduct.getOrDefault(productCode, List.of());
      int index = versionNo - 1;
      if (index < 0 || index >= list.size()) {
        throw new ResourceNotFoundException("ProductVersion", productCode + " v" + versionNo);
      }
      if (list.get(index).status() != ProductVersionStatus.DRAFT) {
        throw new BusinessRuleException(
            "VERSION_NOT_DRAFT", "Version " + versionNo + " of " + productCode + " is not a draft");
      }
      ProductVersionView v = draft(spec, versionNo);
      list.set(index, v);
      return v.ref();
    }

    private static ProductVersionView draft(PackageSpec spec, int versionNo) {
      String name = spec.newProduct() == null ? spec.productCode() : spec.newProduct().name();
      return new ProductVersionView(
          spec.productCode(),
          name,
          versionNo,
          ProductVersionStatus.DRAFT,
          spec.dates(),
          null,
          spec.rateScheme(),
          spec.coverages(),
          spec.insurers(),
          spec.insurerTerms(),
          spec.origin(),
          new ProductVersionView.Checkpoint(null, null, null, null, null));
    }
  }

  /** Stub set-up: DRAFT versions in memory. */
  static final class StubPackageSetup implements PackageSetupService {

    private final InMemoryVersions versions;

    StubPackageSetup(InMemoryVersions versions) {
      this.versions = versions;
    }

    @Override
    public VersionRef createDraftVersion(PackageSpec spec) {
      warn("createDraftVersion");
      if (spec.productCode() == null || spec.productCode().isBlank()) {
        throw new BusinessRuleException("PRODUCT_CODE_REQUIRED", "Enter the product code");
      }
      return versions.add(spec);
    }

    @Override
    public VersionRef updateDraftVersion(String productCode, int versionNo, PackageSpec spec) {
      warn("updateDraftVersion");
      return versions.replace(productCode, versionNo, spec);
    }

    @Override
    public void retireProduct(String productCode, PackageSpec.Origin origin) {
      warn("retireProduct");
    }
  }

  /** Stub reads: only the DRAFT versions of the stub set-up; nothing is ever released. */
  static final class StubVersionQuery implements ProductVersionQueryService {

    private final InMemoryVersions versions;

    StubVersionQuery(InMemoryVersions versions) {
      this.versions = versions;
    }

    @Override
    public Optional<ProductVersionView> current(String productCode) {
      warn("current");
      return Optional.empty();
    }

    @Override
    public Optional<ProductVersionView> inForce(String productCode, LocalDate date) {
      warn("inForce");
      return Optional.empty();
    }

    @Override
    public Optional<ProductVersionView> version(String productCode, int versionNo) {
      warn("version");
      return versions.of(productCode).stream().filter(v -> v.versionNo() == versionNo).findFirst();
    }

    @Override
    public List<ProductVersionView> versions(String productCode) {
      warn("versions");
      return versions.of(productCode).stream()
          .sorted(Comparator.comparingInt(ProductVersionView::versionNo).reversed())
          .toList();
    }

    @Override
    public List<ProductVersionView> packagesExpiring(Long companyId, int withinDays) {
      warn("packagesExpiring");
      return List.of();
    }
  }
}
