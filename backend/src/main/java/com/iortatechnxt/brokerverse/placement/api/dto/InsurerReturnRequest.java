package com.iortatechnxt.brokerverse.placement.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A placement returned by the insurer (BRNB.034).
 *
 * @param reasonCode reason (list RETURN_REASON)
 * @param remarks insurer remarks
 */
public record InsurerReturnRequest(@NotBlank String reasonCode, @Size(max = 1000) String remarks) {}
