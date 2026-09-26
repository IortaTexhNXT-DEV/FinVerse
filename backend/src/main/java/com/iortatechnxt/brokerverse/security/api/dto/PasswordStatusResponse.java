package com.iortatechnxt.brokerverse.security.api.dto;

import java.time.Instant;

/**
 * The password rules in force and the signed-in user's password dates (UAM-NFR-36; FR-UA-005),
 * shown on My Profile and used by the web client to ask for a due change.
 *
 * @param authMode LOCAL or DIRECTORY (in DIRECTORY mode BDO owns the passwords)
 * @param historyCount previous passwords that may not be reused
 * @param minAgeDays days before a changed password may be changed again
 * @param maxAgeDays days after which a password must be changed (0 = never)
 * @param passwordChangedAt last change, null when unknown
 * @param passwordExpiresAt expiry, null when it never expires
 * @param changeDue whether the password must be changed now
 * @param changeReason RESET or EXPIRED when a change is due
 */
public record PasswordStatusResponse(
    String authMode,
    int historyCount,
    int minAgeDays,
    int maxAgeDays,
    Instant passwordChangedAt,
    Instant passwordExpiresAt,
    boolean changeDue,
    String changeReason) {}
