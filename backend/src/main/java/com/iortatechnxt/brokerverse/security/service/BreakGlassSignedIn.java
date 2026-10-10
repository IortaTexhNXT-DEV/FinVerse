package com.iortatechnxt.brokerverse.security.service;

import java.time.Instant;

/**
 * A break-glass System Administrator signed in with a local password while single sign-on is on
 * (BDOI FRS FRUM.001.06: Information Security is alerted of every such sign-in).
 *
 * @param username the administrator
 * @param at time of the sign-in
 * @param address source address, null when not known
 */
public record BreakGlassSignedIn(String username, Instant at, String address) {}
