package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import java.time.LocalDate;

/**
 * Filter of the Access Requests work list (BRD 1.007.1.1, 1.008; FR-UA-017, FR-UA-018).
 *
 * @param scope work-list tab
 * @param status status, null for all
 * @param type request type, null for all
 * @param text user name, role code or request number fragment, null for all
 * @param requester requester, null for all
 * @param approver chosen or deciding approver, null for all
 * @param from requested on or after, null for no limit
 * @param to requested on or before, null for no limit
 * @param groupProfiles true for group-profile requests only, false for user requests only, null for
 *     both
 */
public record AccessRequestSearch(
    Scope scope,
    AccessRequestStatus status,
    AccessRequestType type,
    String text,
    String requester,
    String approver,
    LocalDate from,
    LocalDate to,
    Boolean groupProfiles) {

  /** Defaults to the whole list the viewer may see. */
  public AccessRequestSearch {
    scope = scope == null ? Scope.ALL : scope;
  }

  /** Work-list tabs (USER_ACCESS_DESIGN section 11.2). */
  public enum Scope {
    /** Requests the viewer created (drafts included). */
    MINE,
    /** Requests waiting for the viewer's approval. */
    ASSIGNED,
    /** Requests waiting for a second approval (UAM_SECOND_APPROVE). */
    SECOND,
    /** Approved group-profile requests waiting for the System Administrator (ROLE_MANAGE). */
    IMPLEMENTATION,
    /**
     * Every request the viewer may see (own requests only without ACCESS_APPROVE, USER_MANAGE or
     * AUDIT_VIEW).
     */
    ALL
  }
}
