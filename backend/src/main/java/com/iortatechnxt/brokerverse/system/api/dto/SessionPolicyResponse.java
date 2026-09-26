package com.iortatechnxt.brokerverse.system.api.dto;

/**
 * Session policy applied by the web client (BRNB.040).
 *
 * @param timeoutMinutes inactivity timeout
 * @param warningSeconds how long before the inactivity sign-out the warning dialog appears (the
 *     warning shows after SESSION_IDLE_WARNING_MINUTES of inactivity)
 * @param expiryWarningMinutes how long before the system-triggered (absolute) sign-out at the token
 *     expiry the client warns the user
 */
public record SessionPolicyResponse(
    int timeoutMinutes, int warningSeconds, int expiryWarningMinutes) {}
