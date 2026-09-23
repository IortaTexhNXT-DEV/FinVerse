package com.iortatechnxt.finverse.security.api.dto;

import java.time.Instant;

/**
 * Login result.
 *
 * @param accessToken bearer token
 * @param expiresAt expiry
 * @param user profile of the logged-in user
 */
public record LoginResponse(String accessToken, Instant expiresAt, UserProfileResponse user) {}
