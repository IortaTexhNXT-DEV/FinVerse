package com.iortatechnxt.brokerverse.alert.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Raised exception (exception log entry): an occurrence of an {@link ExceptionCode} condition on a
 * record. Only one live (open or acknowledged) alert exists per {@code dedupKey}.
 */
@Entity
@Table(name = "alt_alert")
public class Alert extends BaseEntity {

  private static final int MAX_TEXT = 500;

  @Column(name = "exception_code", nullable = false, length = 40, updatable = false)
  private String exceptionCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private AlertSeverity severity;

  @Column(nullable = false, length = 30, updatable = false)
  private String module;

  @Column(name = "company_id", updatable = false)
  private Long companyId;

  @Column(name = "branch_id", updatable = false)
  private Long branchId;

  @Column(name = "entity_type", length = 60, updatable = false)
  private String entityType;

  @Column(name = "entity_id", length = 60, updatable = false)
  private String entityId;

  @Column(nullable = false, length = MAX_TEXT, updatable = false)
  private String message;

  @Column(precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(name = "dedup_key", nullable = false, length = 200, updatable = false)
  private String dedupKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 15)
  private AlertStatus status = AlertStatus.OPEN;

  @Column(name = "raised_at", nullable = false, updatable = false)
  private Instant raisedAt;

  @Column(name = "acknowledged_by", length = 50)
  private String acknowledgedBy;

  @Column(name = "acknowledged_at")
  private Instant acknowledgedAt;

  @Column(name = "resolved_by", length = 50)
  private String resolvedBy;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "status_comment", length = MAX_TEXT)
  private String statusComment;

  protected Alert() {}

  /**
   * Raises an alert.
   *
   * @param code exception code (severity and module are copied)
   * @param facts what happened and where
   * @param raisedAt time
   */
  public Alert(ExceptionCode code, AlertFacts facts, Instant raisedAt) {
    this.exceptionCode = code.getCode();
    this.severity = code.getSeverity();
    this.module = code.getModule();
    this.companyId = facts.companyId();
    this.branchId = facts.branchId();
    this.entityType = facts.entityType();
    this.entityId = facts.entityId();
    this.message = truncate(facts.message());
    this.amount = facts.amount();
    this.dedupKey = facts.dedupKey();
    this.raisedAt = raisedAt;
  }

  /**
   * Acknowledges the alert (someone is handling it).
   *
   * @param user user
   * @param when time
   * @param comment optional comment
   */
  public void acknowledge(String user, Instant when, String comment) {
    if (status != AlertStatus.OPEN) {
      throw new BusinessRuleException("ALERT_NOT_OPEN", "Only open alerts can be acknowledged");
    }
    this.status = AlertStatus.ACKNOWLEDGED;
    this.acknowledgedBy = user;
    this.acknowledgedAt = when;
    this.statusComment = truncate(comment);
  }

  /**
   * Resolves the alert. The condition may be raised again later.
   *
   * @param user user
   * @param when time
   * @param comment resolution comment
   */
  public void resolve(String user, Instant when, String comment) {
    if (status == AlertStatus.RESOLVED) {
      throw new BusinessRuleException("ALERT_RESOLVED", "The alert is already resolved");
    }
    if (acknowledgedBy == null) {
      this.acknowledgedBy = user;
      this.acknowledgedAt = when;
    }
    this.status = AlertStatus.RESOLVED;
    this.resolvedBy = user;
    this.resolvedAt = when;
    this.statusComment = truncate(comment);
  }

  private static String truncate(String text) {
    return text != null && text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text;
  }

  public String getExceptionCode() {
    return exceptionCode;
  }

  public AlertSeverity getSeverity() {
    return severity;
  }

  public String getModule() {
    return module;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public String getMessage() {
    return message;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getDedupKey() {
    return dedupKey;
  }

  public AlertStatus getStatus() {
    return status;
  }

  public Instant getRaisedAt() {
    return raisedAt;
  }

  public String getAcknowledgedBy() {
    return acknowledgedBy;
  }

  public Instant getAcknowledgedAt() {
    return acknowledgedAt;
  }

  public String getResolvedBy() {
    return resolvedBy;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public String getStatusComment() {
    return statusComment;
  }
}
