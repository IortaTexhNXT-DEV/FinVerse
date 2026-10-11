package com.iortatechnxt.brokerverse.audit.api.dto;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.domain.AuditLog;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailQuery.AuditEntry;
import java.time.Instant;

/**
 * Audit trail entry (BDOI FRS FRUM.008.02: timestamp, module, user ID, performed by, role, action,
 * activity, from, to and IP address).
 *
 * @param id id
 * @param occurredAt timestamp
 * @param username acting user (performed by)
 * @param entityType entity type
 * @param entityId entity id (reference number)
 * @param action action
 * @param summary description (activity)
 * @param module module of the record
 * @param windowsId Windows ID of the user (user ID), null when none
 * @param actionLabel action in BDOI's words
 * @param roleNames group profiles of the user at the time of the action
 * @param oldValue value before the change, null when not recorded
 * @param newValue value after the change, null when not recorded
 * @param ipAddress source (IP) address
 * @param subject client or assured's name of the record, null when none
 * @param remarks remarks of the action (approval, return or rejection comment), null when none
 */
public record AuditLogResponse(
    Long id,
    Instant occurredAt,
    String username,
    String entityType,
    String entityId,
    AuditAction action,
    String summary,
    String module,
    String windowsId,
    String actionLabel,
    String roleNames,
    String oldValue,
    String newValue,
    String ipAddress,
    String subject,
    String remarks) {

  /**
   * Maps an entry.
   *
   * @param e entry
   * @return response
   */
  public static AuditLogResponse from(AuditEntry e) {
    AuditLog a = e.log();
    return new AuditLogResponse(
        a.getId(),
        a.getOccurredAt(),
        a.getUsername(),
        a.getEntityType(),
        a.getEntityId(),
        a.getAction(),
        a.getSummary(),
        e.module(),
        e.windowsId(),
        e.actionLabel(),
        a.getRoleNames(),
        a.getOldValue(),
        a.getNewValue(),
        a.getIpAddress(),
        e.subject(),
        a.getRemarks());
  }
}
