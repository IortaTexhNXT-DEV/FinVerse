package com.iortatechnxt.brokerverse.messaging.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Channels chosen for a notification event (RMTID.034).
 *
 * @param inApp in-app notification
 * @param email e-mail
 */
public record PreferenceRequest(@NotNull Boolean inApp, @NotNull Boolean email) {}
