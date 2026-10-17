package com.iortatechnxt.brokerverse.security.domain;

/**
 * Privilege level of a role (group profile). A request that raises a user or a role to a higher
 * level needs a second approval (UAM-NFR-40; the levels per role are UQ07).
 */
public enum PrivilegeLevel {
  LOW,
  STANDARD,
  HIGH,
  ADMIN
}
