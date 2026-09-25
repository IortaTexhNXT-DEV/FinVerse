package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import java.time.LocalDate;

/**
 * Identity of one configuration version (SNSRP-101-108). Screening runs, cases, reviews and STRs
 * store {@link #id()} so that later versions do not change them (SNSRP-104 "applies to new reviews
 * only", FR-SS-010 R4).
 *
 * @param id the version id ({@code scr_config_version.id})
 * @param type the configuration type
 * @param scope the template type for {@link ConfigType#TEMPLATE}, otherwise {@code null}
 * @param versionNo the running number of the version within its type and scope
 * @param effectiveFrom the first day the version is in force
 */
public record ConfigVersionRef(
    Long id, ConfigType type, String scope, int versionNo, LocalDate effectiveFrom) {}
