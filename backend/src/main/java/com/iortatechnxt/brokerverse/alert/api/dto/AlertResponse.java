package com.iortatechnxt.brokerverse.alert.api.dto;

import com.iortatechnxt.brokerverse.alert.domain.Alert;
import com.iortatechnxt.brokerverse.alert.domain.AlertSeverity;
import com.iortatechnxt.brokerverse.alert.domain.AlertStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Alert view.
 *
 * @param id id
 * @param exceptionCode exception code
 * @param severity severity
 * @param module module
 * @param companyId company
 * @param branchId branch
 * @param entityType affected record type
 * @param entityId affected record key
 * @param message message
 * @param amount amount
 * @param status status
 * @param raisedAt raise time
 * @param acknowledgedBy acknowledging user
 * @param acknowledgedAt acknowledgement time
 * @param resolvedBy resolving user
 * @param resolvedAt resolution time
 * @param statusComment last comment
 */
public record AlertResponse(
    Long id,
    String exceptionCode,
    AlertSeverity severity,
    String module,
    Long companyId,
    Long branchId,
    String entityType,
    String entityId,
    String message,
    BigDecimal amount,
    AlertStatus status,
    Instant raisedAt,
    String acknowledgedBy,
    Instant acknowledgedAt,
    String resolvedBy,
    Instant resolvedAt,
    String statusComment) {

  /**
   * Maps an entity.
   *
   * @param a alert
   * @return response
   */
  public static AlertResponse from(Alert a) {
    return new AlertResponse(
        a.getId(),
        a.getExceptionCode(),
        a.getSeverity(),
        a.getModule(),
        a.getCompanyId(),
        a.getBranchId(),
        a.getEntityType(),
        a.getEntityId(),
        a.getMessage(),
        a.getAmount(),
        a.getStatus(),
        a.getRaisedAt(),
        a.getAcknowledgedBy(),
        a.getAcknowledgedAt(),
        a.getResolvedBy(),
        a.getResolvedAt(),
        a.getStatusComment());
  }
}
