package com.iortatechnxt.finverse.system.api.dto;

/**
 * Session policy applied by the web client.
 *
 * @param timeoutMinutes inactivity timeout
 * @param warningSeconds how long before sign-out the warning dialog appears
 */
public record SessionPolicyResponse(int timeoutMinutes, int warningSeconds) {}
