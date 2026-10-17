package com.iortatechnxt.brokerverse.alert.api.dto;

import com.iortatechnxt.brokerverse.alert.domain.AlertSeverity;
import com.iortatechnxt.brokerverse.alert.domain.ExceptionCode;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Exception Codes Master entry.
 *
 * @param code code
 * @param name name
 * @param description description
 * @param module module
 * @param severity severity
 * @param thresholdAmount threshold amount
 * @param thresholdDays threshold days
 * @param active monitored
 * @param updatedBy last modifier
 * @param updatedAt last modification
 */
public record ExceptionCodeResponse(
    String code,
    String name,
    String description,
    String module,
    AlertSeverity severity,
    BigDecimal thresholdAmount,
    Integer thresholdDays,
    boolean active,
    String updatedBy,
    Instant updatedAt) {

  /**
   * Maps an entity.
   *
   * @param c code
   * @return response
   */
  public static ExceptionCodeResponse from(ExceptionCode c) {
    return new ExceptionCodeResponse(
        c.getCode(),
        c.getName(),
        c.getDescription(),
        c.getModule(),
        c.getSeverity(),
        c.getThresholdAmount(),
        c.getThresholdDays(),
        c.isActive(),
        c.getUpdatedBy(),
        c.getUpdatedAt());
  }
}
