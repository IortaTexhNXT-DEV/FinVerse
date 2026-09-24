package com.iortatechnxt.brokerverse.nbadmin.domain;

import java.util.Set;

/**
 * What a user access request asks for.
 *
 * @param type request type
 * @param username user to create or change
 * @param fullName full name (create)
 * @param email e-mail (create)
 * @param roleCodes roles (create, modify roles)
 * @param homeBranchId home branch (create)
 * @param justification business justification
 */
public record AccessRequestContent(
    AccessRequestType type,
    String username,
    String fullName,
    String email,
    Set<String> roleCodes,
    Long homeBranchId,
    String justification) {

  /** Defensive copy. */
  public AccessRequestContent {
    roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
  }
}
