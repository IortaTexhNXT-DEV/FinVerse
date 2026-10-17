package com.iortatechnxt.brokerverse.account.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cancellation of a Free First Year tag.
 *
 * @param reasonCode reason (list FFY_CANCEL_REASON)
 * @param comment comment
 */
public record FfyCancelRequest(
    @NotBlank @Size(max = 40) String reasonCode, @Size(max = 200) String comment) {}
