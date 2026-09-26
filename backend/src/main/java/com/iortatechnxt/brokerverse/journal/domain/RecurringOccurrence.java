package com.iortatechnxt.brokerverse.journal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A generated occurrence of a recurring template. The unique (template, date) key makes generation
 * idempotent: an occurrence is never generated twice, whoever runs it and however often.
 */
@Entity
@Table(name = "jnl_recurring_occurrence")
public class RecurringOccurrence {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "template_id", nullable = false)
  private Long templateId;

  @Column(name = "occurrence_date", nullable = false)
  private LocalDate occurrenceDate;

  @Column(name = "batch_id", nullable = false)
  private Long batchId;

  @Column(name = "reversal_batch_id")
  private Long reversalBatchId;

  @Column(name = "generated_at", nullable = false)
  private Instant generatedAt;

  @Column(name = "generated_by", nullable = false, length = 50)
  private String generatedBy;

  protected RecurringOccurrence() {}

  /**
   * Records an occurrence.
   *
   * @param templateId template
   * @param occurrenceDate occurrence date
   * @param batchId generated journal
   * @param reversalBatchId generated reversing journal (null when none)
   * @param generatedAt timestamp
   * @param generatedBy user or SYSTEM
   */
  public RecurringOccurrence(
      Long templateId,
      LocalDate occurrenceDate,
      Long batchId,
      Long reversalBatchId,
      Instant generatedAt,
      String generatedBy) {
    this.templateId = templateId;
    this.occurrenceDate = occurrenceDate;
    this.batchId = batchId;
    this.reversalBatchId = reversalBatchId;
    this.generatedAt = generatedAt;
    this.generatedBy = generatedBy;
  }

  public Long getId() {
    return id;
  }

  public Long getTemplateId() {
    return templateId;
  }

  public LocalDate getOccurrenceDate() {
    return occurrenceDate;
  }

  public Long getBatchId() {
    return batchId;
  }

  public Long getReversalBatchId() {
    return reversalBatchId;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public String getGeneratedBy() {
    return generatedBy;
  }
}
