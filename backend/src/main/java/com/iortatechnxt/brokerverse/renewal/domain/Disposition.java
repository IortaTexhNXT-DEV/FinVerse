package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A disposition row (BRD 2.004; RENEWAL_DESIGN section 4.3): append-only. A later disposition
 * supersedes it; online, uploaded, matrix, insurer, LAMD and system dispositions write the same
 * history with their source (BRRN.018, RQ14). A matrix decision stores the matrix version and the
 * rule id (BRRN.034 AC 3).
 */
@Entity(name = "RnwDisposition")
@Table(name = "rnw_disposition")
public class Disposition extends BaseEntity {

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private RenewalDisposition code;

  @Column(name = "reason_code", length = 40, updatable = false)
  private String reasonCode;

  @Column(length = 200, updatable = false)
  private String remarks;

  @Column(name = "new_invoice_no", length = 40, updatable = false)
  private String newInvoiceNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private DispositionSource source;

  @Column(name = "matrix_version", updatable = false)
  private Integer matrixVersion;

  @Column(name = "rule_id", updatable = false)
  private Long ruleId;

  @Column(name = "superseded_by")
  private Long supersededBy;

  protected Disposition() {}

  /**
   * Records a disposition.
   *
   * @param candidateId candidate
   * @param current code, reason, source, remarks and new invoice number
   * @param matrixVersion matrix version of a matrix decision, else null
   * @param ruleId matrix rule of a matrix decision, else null
   */
  public Disposition(
      Long candidateId, CurrentDisposition current, Integer matrixVersion, Long ruleId) {
    this.candidateId = candidateId;
    this.code = current.code();
    this.reasonCode = current.reasonCode();
    this.remarks = current.remarks();
    this.newInvoiceNo = current.newInvoiceNo();
    this.source = current.source();
    this.matrixVersion = matrixVersion;
    this.ruleId = ruleId;
  }

  /**
   * Marks the row superseded by a later disposition.
   *
   * @param laterId later row
   */
  public void supersede(Long laterId) {
    this.supersededBy = laterId;
  }

  /**
   * The row as the current disposition of the candidate.
   *
   * @return current disposition
   */
  public CurrentDisposition current() {
    return new CurrentDisposition(code, reasonCode, source, remarks, newInvoiceNo);
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public RenewalDisposition getCode() {
    return code;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public String getRemarks() {
    return remarks;
  }

  public String getNewInvoiceNo() {
    return newInvoiceNo;
  }

  public DispositionSource getSource() {
    return source;
  }

  public Integer getMatrixVersion() {
    return matrixVersion;
  }

  public Long getRuleId() {
    return ruleId;
  }

  public Long getSupersededBy() {
    return supersededBy;
  }
}
