package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A validated batch of BIR 2307 certificates of one insurer (CSHID.027): its transaction report is
 * posted and routed to Disbursement, which releases the certificates to the insurer (DBMID.001).
 */
@Entity
@Table(name = "csh_cwt_batch")
public class CwtBatch extends BaseEntity {

  /** Report posted, not routed. */
  public static final String REPORT_POSTED = "REPORT_POSTED";

  /** Routed to Disbursement. */
  public static final String WITH_DISBURSEMENT = "WITH_DISBURSEMENT";

  /** Released to the insurer. */
  public static final String RELEASED = "RELEASED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "batch_no", nullable = false, length = 30, updatable = false)
  private String batchNo;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "tag_count", nullable = false, updatable = false)
  private int tagCount;

  @Column(name = "total_amount", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal totalAmount;

  @Column(nullable = false, length = 20)
  private String status = REPORT_POSTED;

  @Column(name = "routed_at")
  private Instant routedAt;

  @Column(name = "disbursement_request_no", length = 30)
  private String disbursementRequestNo;

  @Column(name = "released_at")
  private Instant releasedAt;

  protected CwtBatch() {}

  /**
   * Creates a batch.
   *
   * @param companyId company
   * @param batchNo CWB- number
   * @param insurerCode insurer
   * @param tagCount number of certificates
   * @param totalAmount total 2307 amount
   */
  public CwtBatch(
      Long companyId, String batchNo, String insurerCode, int tagCount, BigDecimal totalAmount) {
    this.companyId = companyId;
    this.batchNo = batchNo;
    this.insurerCode = insurerCode;
    this.tagCount = tagCount;
    this.totalAmount = totalAmount;
  }

  /**
   * Records the routing to Disbursement.
   *
   * @param requestNo Disbursement request
   * @param at time
   */
  public void routed(String requestNo, Instant at) {
    this.disbursementRequestNo = requestNo;
    this.routedAt = at;
    this.status = WITH_DISBURSEMENT;
  }

  /**
   * Records the release to the insurer.
   *
   * @param at time
   */
  public void released(Instant at) {
    this.releasedAt = at;
    this.status = RELEASED;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public int getTagCount() {
    return tagCount;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public String getStatus() {
    return status;
  }

  public Instant getRoutedAt() {
    return routedAt;
  }

  public String getDisbursementRequestNo() {
    return disbursementRequestNo;
  }

  public Instant getReleasedAt() {
    return releasedAt;
  }
}
