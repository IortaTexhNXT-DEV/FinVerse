package com.iortatechnxt.brokerverse.nbadmin.domain;

/** Whom a user access request is about (cross-BRD decision D7; USER_ACCESS_DESIGN section 4.4). */
public enum AccessUserType {
  /** A BIBS user ({@code sec_user}). */
  INTERNAL,
  /** A portal user of an insurer or client, provisioned through {@code ExternalUserProvisioner}. */
  EXTERNAL
}
