package com.iortatechnxt.brokerverse.nbadmin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * The party and portal role of an external (portal) user request (decision D7; USER_ACCESS_DESIGN
 * section 4.4).
 *
 * @param kind insurer or client
 * @param code code of the insurer or client
 * @param portalRole portal role (INSURER_USER, CLIENT_HR)
 */
@Embeddable
public record ExternalParty(
    @Enumerated(EnumType.STRING) @Column(name = "party_kind", length = 10) ExternalPartyKind kind,
    @Column(name = "party_code", length = 40) String code,
    @Column(name = "portal_role", length = 40) String portalRole) {

  /** Blank values are absent. */
  public ExternalParty {
    code = code == null || code.isBlank() ? null : code.trim();
    portalRole = portalRole == null || portalRole.isBlank() ? null : portalRole.trim();
  }
}
