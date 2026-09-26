package com.iortatechnxt.brokerverse.storage.api.dto;

import com.iortatechnxt.brokerverse.storage.domain.HoldAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to place or release a legal hold.
 *
 * @param action PLACE or RELEASE
 * @param reason reason
 */
public record LegalHoldCommand(
    @NotNull HoldAction action, @NotBlank @Size(max = 500) String reason) {}
