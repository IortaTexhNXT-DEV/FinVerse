package com.iortatechnxt.brokerverse.productmaint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Insert-only snapshot of a package insurer response after each change (PMADD04: every revision and
 * outcome change is logged).
 */
@Entity
@Table(name = "pm_insurer_response_history")
public class PackageResponseHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "response_id", nullable = false, updatable = false)
  private Long responseId;

  @Column(nullable = false, updatable = false)
  private int revision;

  @Column(nullable = false, length = 30, updatable = false)
  private String outcome;

  @Column(precision = 19, scale = 8, updatable = false)
  private BigDecimal rate;

  @Column(name = "minimum_premium", precision = 19, scale = 2, updatable = false)
  private BigDecimal minimumPremium;

  @Column(nullable = false, columnDefinition = "text", updatable = false)
  private String terms;

  @Column(length = 2000, updatable = false)
  private String conditions;

  @Column(name = "valid_until", updatable = false)
  private LocalDate validUntil;

  @Column(length = 1000, updatable = false)
  private String remarks;

  @Column(name = "response_document_id", updatable = false)
  private Long responseDocumentId;

  @Column(name = "changed_by", nullable = false, length = 50, updatable = false)
  private String changedBy;

  @Column(name = "changed_at", nullable = false, updatable = false)
  private Instant changedAt;

  protected PackageResponseHistory() {}

  /**
   * Snapshot of a response as it is now.
   *
   * @param r response
   * @param user who changed it
   * @param when when
   */
  public PackageResponseHistory(PackageInsurerResponse r, String user, Instant when) {
    this.responseId = r.getId();
    this.revision = r.getRevision();
    this.outcome = r.getOutcome();
    this.rate = r.getRate();
    this.minimumPremium = r.getMinimumPremium();
    this.terms = r.getTerms();
    this.conditions = r.getConditions();
    this.validUntil = r.getValidUntil();
    this.remarks = r.getRemarks();
    this.responseDocumentId = r.getResponseDocumentId();
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

  public String getOutcome() {
    return outcome;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public BigDecimal getMinimumPremium() {
    return minimumPremium;
  }

  public String getTerms() {
    return terms;
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

  public Long getResponseDocumentId() {
    return responseDocumentId;
  }

  public String getChangedBy() {
    return changedBy;
  }

  public Instant getChangedAt() {
    return changedAt;
  }
}
