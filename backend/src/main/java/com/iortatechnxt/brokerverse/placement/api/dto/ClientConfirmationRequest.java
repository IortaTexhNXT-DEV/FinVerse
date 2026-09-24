package com.iortatechnxt.brokerverse.placement.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The client's confirmation of an Other Lines account (BRD 2.3.1).
 *
 * @param channel how the client confirmed (list CLIENT_CONFIRMATION_CHANNEL)
 * @param remarks remarks
 * @param attachmentId supporting document attached to the account, optional
 */
public record ClientConfirmationRequest(
    @NotBlank String channel, @Size(max = 500) String remarks, Long attachmentId) {}
