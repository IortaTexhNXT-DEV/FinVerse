package com.iortatechnxt.brokerverse.security.domain;

/**
 * Who changed access and on which authority.
 *
 * @param requestNo number of the approved access request, null for a direct change
 * @param doneBy user who applied the change (or SYSTEM)
 * @param approvedBy approver of the request, null for a direct change
 */
public record AccessChangeSource(String requestNo, String doneBy, String approvedBy) {}
