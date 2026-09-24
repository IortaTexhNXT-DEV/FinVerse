package com.iortatechnxt.brokerverse.quotation.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * One version of a quotation's content (BRNB.020), stored as JSON. A version is frozen when the
 * quotation is submitted and never changes afterwards; the next change opens a new version.
 */
@Entity
@Table(name = "quo_version")
public class QuotationVersion extends BaseEntity {

  @Column(name = "quotation_id", nullable = false, updatable = false)
  private Long quotationId;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "total_sum_insured", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalSumInsured = BigDecimal.ZERO;

  @Column(name = "gross_premium", precision = 19, scale = 2)
  private BigDecimal grossPremium;

  @Column(nullable = false)
  private boolean frozen;

  @Column(name = "frozen_by", length = 50)
  private String frozenBy;

  @Column(name = "frozen_at")
  private Instant frozenAt;

  protected QuotationVersion() {}

  /**
   * Creates a version with its content.
   *
   * @param quotationId quotation
   * @param versionNo version number
   * @param json content as JSON
   * @param sumInsured total sum insured
   * @param gross gross premium, null when not rated
   */
  public QuotationVersion(
      Long quotationId, int versionNo, String json, BigDecimal sumInsured, BigDecimal gross) {
    this.quotationId = quotationId;
    this.versionNo = versionNo;
    store(json, sumInsured, gross);
  }

  /**
   * Replaces the content of an open version.
   *
   * @param json content as JSON
   * @param sumInsured total sum insured
   * @param gross gross premium, null when not rated
   */
  public void write(String json, BigDecimal sumInsured, BigDecimal gross) {
    if (frozen) {
      throw new BusinessRuleException(
          "QUOTATION_VERSION_FROZEN", "Version " + versionNo + " was submitted and is read-only");
    }
    store(json, sumInsured, gross);
  }

  private void store(String json, BigDecimal sumInsured, BigDecimal gross) {
    this.content = json;
    this.totalSumInsured = sumInsured;
    this.grossPremium = gross;
  }

  /**
   * Freezes the version (submission).
   *
   * @param user submitter
   * @param when time
   */
  public void freeze(String user, Instant when) {
    this.frozen = true;
    this.frozenBy = user;
    this.frozenAt = when;
  }

  public Long getQuotationId() {
    return quotationId;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public String getContent() {
    return content;
  }

  public BigDecimal getTotalSumInsured() {
    return totalSumInsured;
  }

  public BigDecimal getGrossPremium() {
    return grossPremium;
  }

  public boolean isFrozen() {
    return frozen;
  }

  public String getFrozenBy() {
    return frozenBy;
  }

  public Instant getFrozenAt() {
    return frozenAt;
  }
}
