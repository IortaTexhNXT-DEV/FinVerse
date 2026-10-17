package com.iortatechnxt.brokerverse.security.service;

/**
 * Published (at most once a minute per store) when a store the sign-in security depends on cannot
 * be read: the token denylist, the session log or the rate limit counters. The requests concerned
 * are refused or checked against the database instead; the alert module raises an alert for the
 * operators.
 *
 * @param store the store: {@code denylist}, {@code sessions} or {@code counters}
 * @param consequence what the application does meanwhile
 * @param error the error of the store
 */
public record SecurityStoreUnavailable(String store, String consequence, String error) {}
