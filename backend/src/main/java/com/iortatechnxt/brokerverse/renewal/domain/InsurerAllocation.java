package com.iortatechnxt.brokerverse.renewal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** The share of an insurer in a renewal account allocated to several insurers (FRRN.014.03). */
@Entity
@Table(name = "rnw_insurer_allocation")
public class InsurerAllocation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "share_percent", nullable = false, precision = 9, scale = 4, updatable = false)
  private BigDecimal sharePercent;

  @Column(name = "coverage_amount", precision = 19, scale = 2, updatable = false)
  private BigDecimal coverageAmount;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by", nullable = false, length = 50, updatable = false)
  private String createdBy;

  protected InsurerAllocation() {}

  /**
   * A share.
   *
   * @param candidateId renewal
   * @param insurerCode insurer
   * @param sharePercent share in percent
   * @param coverageAmount coverage amount, may be null
   * @param createdBy user
   * @param createdAt time
   */
  public InsurerAllocation(
      Long candidateId,
      String insurerCode,
      BigDecimal sharePercent,
      BigDecimal coverageAmount,
      String createdBy,
      Instant createdAt) {
    this.candidateId = candidateId;
    this.insurerCode = insurerCode;
    this.sharePercent = sharePercent;
    this.coverageAmount = coverageAmount;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public BigDecimal getSharePercent() {
    return sharePercent;
  }

  public BigDecimal getCoverageAmount() {
    return coverageAmount;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
