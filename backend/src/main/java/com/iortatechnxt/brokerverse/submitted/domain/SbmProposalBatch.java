package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A batch of renewal proposals SBP-yyyy-nnnnnn (FR-SP-066). */
@Entity
@Table(name = "sbm_proposal_batch")
public class SbmProposalBatch extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "batch_no", nullable = false, updatable = false, length = 30)
  private String batchNo;

  protected SbmProposalBatch() {}

  /**
   * A new batch.
   *
   * @param companyId company
   * @param batchNo number
   */
  public SbmProposalBatch(Long companyId, String batchNo) {
    this.companyId = companyId;
    this.batchNo = batchNo;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBatchNo() {
    return batchNo;
  }
}
