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
 * The one-time ticket of a single sign-on (V1182): the web client redeems it for the session once
 * the identity provider answered. Only its SHA-256 is kept.
 */
@Entity
@Table(name = "sec_sso_ticket")
public class SsoTicket {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(name = "ticket_hash", nullable = false, updatable = false, length = 64)
  private String ticketHash;

  @Column(nullable = false, updatable = false, length = 50)
  private String username;

  @Column(nullable = false, updatable = false, length = 10)
  private String method;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  protected SsoTicket() {}

  /**
   * A new ticket.
   *
   * @param ticketHash SHA-256 of the ticket
   * @param username user
   * @param method OIDC or SAML
   * @param createdAt time
   * @param expiresAt end of validity
   */
  public SsoTicket(
      String ticketHash, String username, String method, Instant createdAt, Instant expiresAt) {
    this.ticketHash = ticketHash;
    this.username = username;
    this.method = method;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  /**
   * Uses the ticket when it is still unused and valid.
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

  public String getUsername() {
    return username;
  }

  public String getMethod() {
    return method;
  }
}
