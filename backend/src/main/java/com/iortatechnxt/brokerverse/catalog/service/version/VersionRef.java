package com.iortatechnxt.brokerverse.catalog.service.version;

import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionStatus;
import java.time.LocalDate;

/**
 * Reference to one version of a packaged product (BRPM.006/007): what {@link PackageSetupService}
 * returns and what {@code productmaint} stores on the package request ({@code
 * resulting_version_no}).
 *
 * @param productCode risk code
 * @param versionNo version number (1..n per product)
 * @param status version status
 * @param effectiveFrom date the version sells from once released
 */
public record VersionRef(
    String productCode, int versionNo, ProductVersionStatus status, LocalDate effectiveFrom) {}
