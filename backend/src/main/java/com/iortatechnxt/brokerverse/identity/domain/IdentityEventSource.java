package com.iortatechnxt.brokerverse.identity.domain;

/** Where a provisioning event comes from. */
public enum IdentityEventSource {
  /** The provisioning interface of UIDM-ISC. */
  UIDM_ISC("UIDM-ISC"),
  /** A status change of the Enterprise SSO platform. */
  ENTERPRISE_SSO("Enterprise SSO"),
  /** An on-demand synchronisation or a creation from an Enterprise SSO account in the system. */
  ON_DEMAND("On-demand synchronisation");

  private final String label;

  IdentityEventSource(String label) {
    this.label = label;
  }

  /**
   * Name shown to the users and recorded as the actor of the change.
   *
   * @return label
   */
  public String label() {
    return label;
  }
}
