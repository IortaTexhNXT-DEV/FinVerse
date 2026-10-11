package com.iortatechnxt.brokerverse.configpromo.api.dto;

import com.iortatechnxt.brokerverse.configpromo.engine.ManifestDataset;
import java.util.List;

/**
 * A package with the datasets of its manifest.
 *
 * @param pkg package
 * @param datasets datasets with rows and checksums
 */
public record PackageDetailResponse(PackageResponse pkg, List<ManifestDataset> datasets) {}
