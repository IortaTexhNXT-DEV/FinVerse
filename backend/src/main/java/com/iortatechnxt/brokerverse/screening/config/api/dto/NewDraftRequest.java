package com.iortatechnxt.brokerverse.screening.config.api.dto;

import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import jakarta.validation.constraints.NotNull;

/**
 * Opens a draft of a configuration type (FR-SS-010 "New Draft").
 *
 * @param companyId company
 * @param type configuration type
 * @param scope template type for TEMPLATE, otherwise blank
 */
public record NewDraftRequest(@NotNull Long companyId, @NotNull ConfigType type, String scope) {}
