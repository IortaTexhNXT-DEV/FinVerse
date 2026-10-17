package com.iortatechnxt.brokerverse.configpromo.api.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * An export.
 *
 * @param datasets dataset codes; empty = every dataset selected by default
 * @param includeUsers whether users are exported (without passwords or second factor)
 * @param baselineId baseline for an incremental package of the datasets changed since, null for a
 *     full package
 * @param description purpose
 */
public record ExportRequest(
    List<String> datasets,
    boolean includeUsers,
    Long baselineId,
    @Size(max = 500) String description) {}
