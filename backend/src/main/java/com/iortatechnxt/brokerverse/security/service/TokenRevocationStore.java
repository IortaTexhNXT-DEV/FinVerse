package com.iortatechnxt.brokerverse.security.service;

import java.time.Instant;

/**
 * Port: the denylist of revoked access tokens, shared by every instance. An entry lives until the
 * token would have expired anyway, so the list never grows beyond the tokens still valid.
 *
 * <p>Implemented on Redis ({@code brokerverse.redis.enabled=true}) or on the table {@code
 * sec_revoked_token} (module {@code sharedstate}).
 */
public interface TokenRevocationStore {

  /**
   * Revokes a token until its expiry.
   *
   * @param tokenId token id ({@code jti})
   * @param username token subject
   * @param expiresAt token expiry (the entry is dropped afterwards)
   */
  void revoke(String tokenId, String username, Instant expiresAt);

  /**
   * Whether a token was revoked.
   *
   * @param tokenId token id ({@code jti})
   * @return true when revoked and not yet expired
   */
  boolean isRevoked(String tokenId);
}
