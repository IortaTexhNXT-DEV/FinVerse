package com.iortatechnxt.brokerverse.audit.api.dto;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.domain.AuditLog;
import java.time.Instant;

/**
 * Audit trail entry.
 *
 * @param id id
 * @param occurredAt timestamp
 * @param username acting user
 * @param entityType entity type
 * @param entityId entity id
 * @param action action
 * @param summary description
 */
public record AuditLogResponse(
    Long id,
    Instant occurredAt,
    String username,
    String entityType,
    String entityId,
    AuditAction action,
    String summary) {

  /**
   * Maps an entity.
   *
   * @param a entity
   * @return response
   */
  public static AuditLogResponse from(AuditLog a) {
    return new AuditLogResponse(
        a.getId(),
        a.getOccurredAt(),
        a.getUsername(),
        a.getEntityType(),
        a.getEntityId(),
        a.getAction(),
        a.getSummary());
  }
}
