package com.iortatechnxt.brokerverse.security.service.mfa;

/**
 * Published when a reset of a user's second factor was approved and applied: the user enrols an
 * authenticator app again at the next sign-in.
 *
 * @param username user
 */
public record MfaResetApplied(String username) {}
