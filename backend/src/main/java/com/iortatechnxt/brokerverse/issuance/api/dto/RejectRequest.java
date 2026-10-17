package com.iortatechnxt.brokerverse.issuance.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Rejection of a received e-policy at review.
 *
 * @param reasonCode reason (list EPOLICY_REJECT_REASON)
 * @param remarks remarks
 */
public record RejectRequest(@NotBlank String reasonCode, @Size(max = 500) String remarks) {}
