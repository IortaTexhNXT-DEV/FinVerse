package com.iortatechnxt.brokerverse.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * A sign-in sent to the identity provider (V1182): the SHA-256 of its state, the PKCE verifier and
 * nonce (OIDC) or the request ID (SAML). The answer of the provider is accepted once, within the
 * validity.
 */
@Entity
@Table(name = "sec_sso_request")
public class SsoRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(name = "state_hash", nullable = false, updatable = false, length = 64)
  private String stateHash;

  @Column(nullable = false, updatable = false, length = 10)
  private String protocol;

  @Column(name = "code_verifier", updatable = false, length = 128)
  private String codeVerifier;

  @Column(updatable = false, length = 128)
  private String nonce;

  @Column(name = "request_id", updatable = false, length = 80)
  private String requestId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  protected SsoRequest() {}

  /**
   * A new request.
   *
   * @param stateHash SHA-256 of the state
   * @param protocol OIDC or SAML
   * @param codeVerifier PKCE verifier (OIDC)
   * @param nonce nonce of the ID token (OIDC)
   * @param requestId ID of the AuthnRequest (SAML)
   * @param createdAt time
   * @param expiresAt end of validity
   */
  @SuppressWarnings("java:S107") // one column each
  public SsoRequest(
      String stateHash,
      String protocol,
      String codeVerifier,
      String nonce,
      String requestId,
      Instant createdAt,
      Instant expiresAt) {
    this.stateHash = stateHash;
    this.protocol = protocol;
    this.codeVerifier = codeVerifier;
    this.nonce = nonce;
    this.requestId = requestId;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  /**
   * Uses the request when it is still unused and valid.
   *
   * @param now time
   * @return true when it could be used
   */
  public boolean use(Instant now) {
    if (usedAt != null || !now.isBefore(expiresAt)) {
      return false;
    }
    usedAt = now;
    return true;
  }

  public String getProtocol() {
    return protocol;
  }

  public String getCodeVerifier() {
    return codeVerifier;
  }

  public String getNonce() {
    return nonce;
  }

  public String getRequestId() {
    return requestId;
  }
}
