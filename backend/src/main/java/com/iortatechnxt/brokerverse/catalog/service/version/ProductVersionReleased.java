package com.iortatechnxt.brokerverse.catalog.service.version;

import java.time.LocalDate;

/**
 * A package version passed the validation checkpoint and was released (PMADD06, BRPM.015/022).
 * Published by the catalog <b>after commit</b> of the validation; {@code productmaint} listens with
 * {@code @TransactionalEventListener} (or {@code @EventListener} on the committed event) and moves
 * the request whose number is {@code sourceRequestNo} from FOR_VALIDATION to RELEASED (system
 * action {@code version_released} of PM_PACKAGE_REQUEST), then drafts the advisory.
 *
 * @param productCode risk code
 * @param versionNo released version
 * @param effectiveFrom date the version sells from
 * @param sourceRequestNo package request number, null for a catalog-only version
 * @param validatedBy validator (never the maker)
 */
public record ProductVersionReleased(
    String productCode,
    int versionNo,
    LocalDate effectiveFrom,
    String sourceRequestNo,
    String validatedBy) {}
