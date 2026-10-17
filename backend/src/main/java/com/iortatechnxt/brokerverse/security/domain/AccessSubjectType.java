package com.iortatechnxt.brokerverse.security.domain;

/**
 * Kind of subject of an access change: an internal user, a role (group profile), or an external
 * (portal) user provisioned through an access request (decision D7, V1062).
 */
public enum AccessSubjectType {
  USER,
  ROLE,
  EXTERNAL_USER
}
