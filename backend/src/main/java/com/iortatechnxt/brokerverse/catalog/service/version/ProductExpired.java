package com.iortatechnxt.brokerverse.catalog.service.version;

import java.time.LocalDate;

/**
 * A package expired: its package end date passed and no newer version is released, so its version
 * became EXPIRED and the product's lifecycle EXPIRED (BRPM.006/017). Published by the catalog
 * <b>after commit</b> of the expiry step of the daily PACKAGE_EXPIRY_MONITOR job; {@code
 * productmaint} notifies TSU and MBS and the catalog raises INCENTIVE_PRODUCT_INACTIVE for active
 * incentive criteria of the product (PMADD08).
 *
 * @param productCode risk code
 * @param versionNo expired version
 * @param packageEndDate package end date that passed
 */
public record ProductExpired(String productCode, int versionNo, LocalDate packageEndDate) {}
