package com.iortatechnxt.brokerverse.security.api.dto;

import java.time.Instant;

/**
 * A renewed access token.
 *
 * @param accessToken bearer token
 * @param accessTokenExpiresAt expiry of the access token
 * @param expiresAt end of the sign-in session
 */
public record RefreshResponse(
    String accessToken, Instant accessTokenExpiresAt, Instant expiresAt) {}
