package com.iortatechnxt.brokerverse.security.service.mfa;

/**
 * Published when a reset of a user's second factor waits for the approval of a holder of
 * MFA_RESET_APPROVE (other than the requester).
 *
 * @param requestId request
 * @param username user whose second factor is reset
 * @param requestedBy requester
 * @param reason why
 */
public record MfaResetRequested(
    Long requestId, String username, String requestedBy, String reason) {}
