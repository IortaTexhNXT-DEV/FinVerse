package com.iortatechnxt.brokerverse.catalog.service.version;

/**
 * A package version was returned from FOR_VALIDATION to DRAFT with a reason (PMADD06). Published by
 * the catalog <b>after commit</b>; {@code productmaint} moves the request whose number is {@code
 * sourceRequestNo} back to WITH_MBS (system action {@code version_returned} of PM_PACKAGE_REQUEST).
 *
 * @param productCode risk code
 * @param versionNo returned version (now DRAFT)
 * @param reason reason given by the validator
 * @param sourceRequestNo package request number, null for a catalog-only version
 * @param returnedBy validator who returned it
 */
public record ProductVersionReturned(
    String productCode, int versionNo, String reason, String sourceRequestNo, String returnedBy) {}
