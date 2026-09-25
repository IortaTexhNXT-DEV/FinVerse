package com.iortatechnxt.brokerverse.catalog.service.version;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Read access to package product versions (BRPM.006/007/017; PRODUCT_MAINTENANCE_DESIGN sections
 * 5.1, 5.2 and 9.1). Implemented by the catalog (P1-A); read by {@code productmaint} (request
 * pre-fill, expiry monitor, package status report) and by rating. Read-only: implementations run in
 * a read-only transaction and need no permission of their own (callers check theirs). Non-packaged
 * products have no versions, so every method returns empty for them.
 *
 * <p>Until the catalog implementation exists, {@link PackageVersionStubDefaults} registers an
 * in-memory stub that knows only the versions its stub {@link PackageSetupService} created.
 */
public interface ProductVersionQueryService {

  /**
   * The version that prices new business today (the business date of the injected clock): the
   * RELEASED version in force, whatever the period start of the transaction (BRPM.007).
   *
   * @param productCode risk code
   * @return the current version, empty when the product has none (not packaged, or expired /
   *     retired)
   */
  Optional<ProductVersionView> current(String productCode);

  /**
   * The RELEASED or SUPERSEDED version in force on a date (renewal and endorsement look-ups).
   *
   * @param productCode risk code
   * @param date date
   * @return the version in force, empty when none
   */
  Optional<ProductVersionView> inForce(String productCode, LocalDate date);

  /**
   * One version, whatever its status.
   *
   * @param productCode risk code
   * @param versionNo version number
   * @return the version, empty when unknown
   */
  Optional<ProductVersionView> version(String productCode, int versionNo);

  /**
   * Every version of a product (history, BRPM.006), newest first.
   *
   * @param productCode risk code
   * @return versions, empty for a non-packaged product
   */
  List<ProductVersionView> versions(String productCode);

  /**
   * Current RELEASED versions whose package end date falls between today and today plus {@code
   * withinDays} (inclusive), soonest first (expiry monitor and Package Expiry list, BRPM.017).
   *
   * @param companyId company (the catalog is shared by the companies today; kept for the
   *     multi-company seam)
   * @param withinDays look-ahead in days (0 = ending today)
   * @return expiring versions
   */
  List<ProductVersionView> packagesExpiring(Long companyId, int withinDays);
}
