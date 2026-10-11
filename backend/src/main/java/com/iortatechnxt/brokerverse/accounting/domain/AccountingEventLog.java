package com.iortatechnxt.brokerverse.accounting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** Register of processed accounting events (event monitor and failed-event report). */
@Entity
@Table(name = "acc_event_log")
public class AccountingEventLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "event_type", nullable = false, length = 40)
  private String eventType;

  @Column(name = "source_module", nullable = false, length = 30)
  private String sourceModule;

  @Column(name = "source_reference", nullable = false, length = 80)
  private String sourceReference;

  @Column(length = 60)
  private String reference;

  @Column(name = "value_date", nullable = false)
  private LocalDate valueDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private EventStatus status;

  @Column(name = "rule_id")
  private Long ruleId;

  @Column(name = "batch_no", length = 40)
  private String batchNo;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Column(nullable = false, length = 1000)
  private String amounts;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;

  @Column(name = "processed_by", nullable = false, length = 50)
  private String processedBy;

  protected AccountingEventLog() {}

  /**
   * Creates a log record.
   *
   * @param entry values
   */
  public AccountingEventLog(EventLogEntry entry) {
    this.companyId = entry.companyId();
    this.eventType = entry.eventType();
    this.sourceModule = entry.sourceModule();
    this.sourceReference = entry.sourceReference();
    this.reference = entry.reference();
    this.valueDate = entry.valueDate();
    this.status = entry.status();
    this.ruleId = entry.ruleId();
    this.batchNo = entry.batchNo();
    this.errorMessage = entry.errorMessage();
    this.amounts = entry.amounts();
    this.processedAt = entry.processedAt();
    this.processedBy = entry.processedBy();
  }

  public Long getId() {
    return id;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getSourceReference() {
    return sourceReference;
  }

  public String getReference() {
    return reference;
  }

  public LocalDate getValueDate() {
    return valueDate;
  }

  public EventStatus getStatus() {
    return status;
  }

  public Long getRuleId() {
    return ruleId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getAmounts() {
    return amounts;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }

  public String getProcessedBy() {
    return processedBy;
  }
}
