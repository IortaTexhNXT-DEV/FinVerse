package com.iortatechnxt.brokerverse.nonpackage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Insert-only snapshot of an insurer response after each change (BRNB.009 version history). */
@Entity
@Table(name = "npk_insurer_response_history")
public class InsurerResponseHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "response_id", nullable = false, updatable = false)
  private Long responseId;

  @Column(nullable = false, updatable = false)
  private int revision;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private ResponseStatus status;

  @Column(precision = 19, scale = 2, updatable = false)
  private BigDecimal premium;

  @Column(precision = 19, scale = 8, updatable = false)
  private BigDecimal rate;

  @Column(length = 1000, updatable = false)
  private String deductibles;

  @Column(length = 2000, updatable = false)
  private String conditions;

  @Column(name = "valid_until", updatable = false)
  private LocalDate validUntil;

  @Column(length = 1000, updatable = false)
  private String remarks;

  @Column(name = "document_id", updatable = false)
  private Long documentId;

  @Column(nullable = false, updatable = false)
  private boolean recommended;

  @Column(name = "changed_by", nullable = false, length = 50, updatable = false)
  private String changedBy;

  @Column(name = "changed_at", nullable = false, updatable = false)
  private Instant changedAt;

  protected InsurerResponseHistory() {}

  /**
   * Snapshot of a response.
   *
   * @param response response after the change
   * @param user user who changed it
   * @param when time
   */
  public InsurerResponseHistory(InsurerResponse response, String user, Instant when) {
    ResponseTerms t = response.terms();
    this.responseId = response.getId();
    this.revision = response.getRevision();
    this.status = t.status();
    this.premium = t.premium();
    this.rate = t.rate();
    this.deductibles = t.deductibles();
    this.conditions = t.conditions();
    this.validUntil = t.validUntil();
    this.remarks = t.remarks();
    this.documentId = response.getDocumentId();
    this.recommended = response.isRecommended();
    this.changedBy = user;
    this.changedAt = when;
  }

  public Long getId() {
    return id;
  }

  public Long getResponseId() {
    return responseId;
  }

  public int getRevision() {
    return revision;
  }

  public ResponseStatus getStatus() {
    return status;
  }

  public BigDecimal getPremium() {
    return premium;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public String getDeductibles() {
    return deductibles;
  }

  public String getConditions() {
    return conditions;
  }

  public LocalDate getValidUntil() {
    return validUntil;
  }

  public String getRemarks() {
    return remarks;
  }

  public Long getDocumentId() {
    return documentId;
  }

  public boolean isRecommended() {
    return recommended;
  }

  public String getChangedBy() {
    return changedBy;
  }

  public Instant getChangedAt() {
    return changedAt;
  }
}
